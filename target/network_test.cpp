#include "tcp_tci.h"
#include "core/platform.h"

#include <cstdint>
#include <cstdio>
#include <cstring>
#include <thread>

// ─── Minimal echo server (raw sockets, runs on background thread) ───

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <winsock2.h>
#include <ws2tcpip.h>
using SockT = SOCKET;
static constexpr SockT BadSock = INVALID_SOCKET;
static void closeS(SockT s) { closesocket(s); }
#else
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
using SockT = int;
static constexpr SockT BadSock = -1;
static void closeS(SockT s) { ::close(s); }
#endif

// ─── Test ───

int main() {
    setvbuf(stdout, nullptr, _IONBF, 0);
    setvbuf(stderr, nullptr, _IONBF, 0);

    RT_LOG_INFO("═══ TcpTCI / NetworkTCI Test ═══\n");

    const int PORT = 19876;

    // Start echo server on a thread
    volatile bool srvReady = false;
    volatile bool srvDone  = false;

    std::thread srv([&]() {
#ifdef _WIN32
        WSADATA wsa;
        WSAStartup(MAKEWORD(2,2), &wsa);
#endif
        SockT srvSock = ::socket(AF_INET, SOCK_STREAM, 0);
        if (srvSock == BadSock) { srvDone = true; return; }
        int opt = 1;
#ifdef _WIN32
        setsockopt(srvSock, SOL_SOCKET, SO_REUSEADDR, (const char*)&opt, sizeof(opt));
#else
        setsockopt(srvSock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
#endif
        struct sockaddr_in addr;
        std::memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_addr.s_addr = INADDR_ANY;
        addr.sin_port = htons(PORT);
        if (::bind(srvSock, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
            closeS(srvSock); srvDone = true; return;
        }
        ::listen(srvSock, 1);
        srvReady = true;

        struct sockaddr_in cli;
#ifdef _WIN32
        int cliLen = sizeof(cli);
#else
        socklen_t cliLen = sizeof(cli);
#endif
        SockT cliSock = ::accept(srvSock, (struct sockaddr*)&cli, &cliLen);
        closeS(srvSock);
        if (cliSock == BadSock) { srvDone = true; return; }

        char buf[256];
        int n;
        while (!srvDone && (n = (int)::recv(cliSock, buf, sizeof(buf), 0)) > 0)
            ::send(cliSock, buf, n, 0);
        closeS(cliSock);
#ifdef _WIN32
        WSACleanup();
#endif
        srvDone = true;
    });

    // Wait for server to be ready
    for (int i = 0; i < 200 && !srvReady && !srvDone; i++)
        std::this_thread::sleep_for(std::chrono::milliseconds(10));

    if (!srvReady) {
        RT_LOG_ERR("[TEST] Server failed to start\n");
        srvDone = true;
        srv.join();
        return 1;
    }
    RT_LOG_INFO("[TEST] Server ready on port %d\n", PORT);

    // TcpTCI client
    bool ok = false;
    {
        rt_plc::TcpTCI tcp("127.0.0.1", PORT, 0);
        tcp.map(0, 8);

        for (int i = 0; i < 100 && !tcp.isConnected(); i++)
            std::this_thread::sleep_for(std::chrono::milliseconds(20));

        if (!tcp.isConnected()) {
            RT_LOG_ERR("[TEST] Client failed to connect\n");
            srvDone = true;
            srv.join();
            return 1;
        }
        RT_LOG_INFO("[TEST] Connected\n");

        // Write → syncOutputs → send to echo server
        rt_plc::ProcessImage img;
        uint8_t txData[8] = {0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF};
        std::memcpy(img.outputs, txData, 8);
        tcp.syncOutputs(img);

        std::this_thread::sleep_for(std::chrono::milliseconds(200));

        // Read back via syncInputs
        tcp.syncInputs(img);
        ok = (std::memcmp(img.inputs, txData, 8) == 0);

        RT_LOG_INFO("[TEST] %s: tx=", ok ? "PASS" : "FAIL");
        for (int i = 0; i < 8; i++) RT_LOG_INFO("%02X", txData[i]);
        RT_LOG_INFO(" rx=");
        for (int i = 0; i < 8; i++) RT_LOG_INFO("%02X", img.inputs[i]);
        RT_LOG_INFO("\n");
    }
    // tcp destroyed → client socket closed → server recv returns 0 → server thread exits
    srv.join();
    RT_LOG_INFO("═══ %s ═══\n", ok ? "ALL PASS" : "FAILED");
    return ok ? 0 : 1;
}
