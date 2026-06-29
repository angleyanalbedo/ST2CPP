#include "network_tci.h"
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

static void echoServer(int port, volatile bool& ready, volatile bool& done) {
#ifdef _WIN32
    WSADATA wsa;
    WSAStartup(MAKEWORD(2,2), &wsa);
#endif
    SockT srv = ::socket(AF_INET, SOCK_STREAM, 0);
    if (srv == BadSock) { done = true; return; }
    int opt = 1;
#ifdef _WIN32
    setsockopt(srv, SOL_SOCKET, SO_REUSEADDR, (const char*)&opt, sizeof(opt));
#else
    setsockopt(srv, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
#endif
    struct sockaddr_in addr;
    std::memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons((uint16_t)port);
    if (::bind(srv, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        closeS(srv);
#ifdef _WIN32
        WSACleanup();
#endif
        done = true; return;
    }
    ::listen(srv, 1);
    ready = true;  // signal that server is listening

    struct sockaddr_in cli;
#ifdef _WIN32
    int cliLen = sizeof(cli);
#else
    socklen_t cliLen = sizeof(cli);
#endif
    SockT c = ::accept(srv, (struct sockaddr*)&cli, &cliLen);
    closeS(srv);
    if (c == BadSock) { done = true; return; }

    // Echo loop
    char buf[256];
    int n;
    while (!done && (n = (int)::recv(c, buf, sizeof(buf), 0)) > 0) {
        ::send(c, buf, n, 0);
    }
    closeS(c);
#ifdef _WIN32
    WSACleanup();
#endif
    done = true;
}

// ─── Test ───

static bool testTcpEcho(int port) {
    volatile bool srvReady = false;
    volatile bool srvDone = false;

    // Start echo server
    std::thread srv(echoServer, port, std::ref(srvReady), std::ref(srvDone));
    while (!srvReady && !srvDone)
        std::this_thread::sleep_for(std::chrono::milliseconds(10));

    if (srvDone) {
        RT_LOG_ERR("[TEST] Server failed to start\n");
        srv.join();
        return false;
    }

    // Create NetworkTCI client
    rt_plc::NetworkTCI net;
    net.begin("127.0.0.1", (uint16_t)port, 0);
    net.mapOutput(0, 0, 8);   // PLC output bytes 0-7 → network bytes 0-7
    net.mapInput(0, 0, 8);    // network bytes 0-7 → PLC input bytes 0-7

    // Wait for connection
    for (int i = 0; i < 100 && !net.isConnected(); i++)
        std::this_thread::sleep_for(std::chrono::milliseconds(20));

    if (!net.isConnected()) {
        RT_LOG_ERR("[TEST] Client failed to connect\n");
        srvDone = true;
        net.end();
        srv.join();
        return false;
    }
    RT_LOG_INFO("[TEST] Connected\n");

    // Write test pattern to ProcessImage outputs, then sync
    rt_plc::ProcessImage img;
    uint8_t txData[8] = {0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF};
    std::memcpy(img.outputs, txData, 8);
    net.syncOutputs(img);

    // Wait for echo
    std::this_thread::sleep_for(std::chrono::milliseconds(200));

    // Read back from ProcessImage inputs
    net.syncInputs(img);
    bool ok = (std::memcmp(img.inputs, txData, 8) == 0);

    RT_LOG_INFO("[TEST] %s: tx=", ok ? "PASS" : "FAIL");
    for (int i = 0; i < 8; i++)
        RT_LOG_INFO("%02X", txData[i]);
    RT_LOG_INFO(" rx=");
    for (int i = 0; i < 8; i++)
        RT_LOG_INFO("%02X", img.inputs[i]);
    RT_LOG_INFO("\n");

    srvDone = true;
    net.end();
    srv.join();
    return ok;
}

int main() {
    RT_LOG_INFO("═══ NetworkTCI Test ═══\n");
    bool ok = testTcpEcho(19876);
    RT_LOG_INFO("═══ %s ═══\n", ok ? "ALL PASS" : "FAILED");
    return ok ? 0 : 1;
}
