#include "network_tci.h"

#include "core/platform.h"

#include <cstring>
#include <thread>

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <winsock2.h>
#include <ws2tcpip.h>
#else
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>
#include <fcntl.h>
#include <cerrno>
#endif

namespace rt_plc {

// ─── Platform socket abstraction ───

#ifdef _WIN32
using SockHandle = SOCKET;
static constexpr SockHandle INVALID_SOCK_HANDLE = INVALID_SOCKET;

static bool isWouldBlock() {
    int e = WSAGetLastError();
    return e == WSAEWOULDBLOCK || e == WSAETIMEDOUT;
}
#else
using SockHandle = int;
static constexpr SockHandle INVALID_SOCK_HANDLE = -1;

static bool isWouldBlock() {
    return errno == EAGAIN || errno == EWOULDBLOCK;
}
#endif

static bool sockInit() {
#ifdef _WIN32
    WSADATA wsa;
    return WSAStartup(MAKEWORD(2, 2), &wsa) == 0;
#else
    return true;
#endif
}

static void sockCleanup() {
#ifdef _WIN32
    WSACleanup();
#endif
}

static bool sockCreate(SockHandle& s) {
    s = ::socket(AF_INET, SOCK_STREAM, 0);
    if (s == INVALID_SOCK_HANDLE) return false;
    int flag = 1;
#ifdef _WIN32
    setsockopt(s, IPPROTO_TCP, TCP_NODELAY, (const char*)&flag, sizeof(flag));
#else
    setsockopt(s, IPPROTO_TCP, TCP_NODELAY, &flag, sizeof(flag));
#endif
    return true;
}

static void sockClose(SockHandle s) {
    if (s == INVALID_SOCK_HANDLE) return;
#ifdef _WIN32
    closesocket(s);
#else
    ::close(s);
#endif
}

static void sockSetNonBlocking(SockHandle s, bool nb) {
#ifdef _WIN32
    u_long mode = nb ? 1 : 0;
    ioctlsocket(s, FIONBIO, &mode);
#else
    int flags = fcntl(s, F_GETFL, 0);
    if (nb) flags |= O_NONBLOCK;
    else    flags &= ~O_NONBLOCK;
    fcntl(s, F_SETFL, flags);
#endif
}

// ─── NetworkTCI ───

NetworkTCI::NetworkTCI()
    : m_inputCount(0)
    , m_outputCount(0)
    , m_txPending(false)
    , m_reconnectMs(3000)
    , m_serverMode(false)
{
    std::memset(m_inputMaps, 0, sizeof(m_inputMaps));
    std::memset(m_outputMaps, 0, sizeof(m_outputMaps));
    m_rxBuf.resize(NET_BUF_SIZE, 0);
    m_txBuf.resize(NET_BUF_SIZE, 0);
}

NetworkTCI::~NetworkTCI() {
    end();
}

// ─── Lifecycle ───

bool NetworkTCI::begin(const char* host, uint16_t port, int reconnectMs) {
    if (m_running.load(std::memory_order_acquire)) return false;
    sockInit();
    m_reconnectMs = reconnectMs;
    m_serverMode = false;
    m_running.store(true, std::memory_order_release);
    m_worker = std::thread(&NetworkTCI::workerClient, this, std::string(host), port);
    return true;
}

bool NetworkTCI::begin(uint16_t port, int reconnectMs) {
    if (m_running.load(std::memory_order_acquire)) return false;
    sockInit();
    m_reconnectMs = reconnectMs;
    m_serverMode = true;
    m_running.store(true, std::memory_order_release);
    m_worker = std::thread(&NetworkTCI::workerServer, this, port);
    return true;
}

void NetworkTCI::end() {
    m_running.store(false, std::memory_order_release);
    if (m_worker.joinable()) m_worker.join();
    sockCleanup();
}

// ─── Mapping ───

void NetworkTCI::mapInput(int plcOffset, int netOffset, int length) {
    if (m_inputCount >= MAX_BYTE_MAPPINGS) return;
    m_inputMaps[m_inputCount].plcOffset = plcOffset;
    m_inputMaps[m_inputCount].netOffset = netOffset;
    m_inputMaps[m_inputCount].length = length;
    m_inputCount++;
}

void NetworkTCI::mapOutput(int plcOffset, int netOffset, int length) {
    if (m_outputCount >= MAX_BYTE_MAPPINGS) return;
    m_outputMaps[m_outputCount].plcOffset = plcOffset;
    m_outputMaps[m_outputCount].netOffset = netOffset;
    m_outputMaps[m_outputCount].length = length;
    m_outputCount++;
}

void NetworkTCI::clearMappings() {
    m_inputCount = 0;
    m_outputCount = 0;
}

// ─── TCI interface (PLC cycle, non-blocking) ───

void NetworkTCI::syncInputs(ProcessImage& img) {
    if (m_inputCount == 0) return;
    std::lock_guard<std::mutex> lock(m_mutex);
    for (int i = 0; i < m_inputCount; i++) {
        const auto& m = m_inputMaps[i];
        int netEnd = m.netOffset + m.length;
        size_t piOff = (size_t)m.plcOffset;
        if (netEnd > (int)m_rxBuf.size()) continue;
        if (piOff + (size_t)m.length > PROCESS_IMAGE_SIZE) continue;
        std::memcpy(img.inputs + piOff, m_rxBuf.data() + m.netOffset, (size_t)m.length);
    }
}

void NetworkTCI::syncOutputs(ProcessImage& img) {
    if (m_outputCount == 0) return;
    std::lock_guard<std::mutex> lock(m_mutex);
    for (int i = 0; i < m_outputCount; i++) {
        const auto& m = m_outputMaps[i];
        int netEnd = m.netOffset + m.length;
        size_t piOff = (size_t)m.plcOffset;
        if (netEnd > (int)m_txBuf.size()) continue;
        if (piOff + (size_t)m.length > PROCESS_IMAGE_SIZE) continue;
        std::memcpy(m_txBuf.data() + m.netOffset, img.outputs + piOff, (size_t)m.length);
    }
    m_txPending = true;
}

// ─── Worker thread: client mode ───

void NetworkTCI::workerClient(std::string host, uint16_t port) {
    RT_LOG_INFO("[NetworkTCI] Client thread started: %s:%u\n", host.c_str(), port);

    while (m_running.load(std::memory_order_acquire)) {
        SockHandle sock = INVALID_SOCK_HANDLE;
        if (!sockCreate(sock)) {
            RT_LOG_ERR("[NetworkTCI] Failed to create socket\n");
            std::this_thread::sleep_for(std::chrono::milliseconds(m_reconnectMs));
            continue;
        }

        struct sockaddr_in addr;
        std::memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_port = htons(port);

        struct hostent* he = gethostbyname(host.c_str());
        if (!he) {
            RT_LOG_ERR("[NetworkTCI] DNS lookup failed: %s\n", host.c_str());
            sockClose(sock);
            std::this_thread::sleep_for(std::chrono::milliseconds(m_reconnectMs));
            continue;
        }
        std::memcpy(&addr.sin_addr, he->h_addr_list[0], (size_t)he->h_length);

        if (::connect(sock, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
            RT_LOG_ERR("[NetworkTCI] connect(%s:%u) failed\n", host.c_str(), port);
            sockClose(sock);
            m_connected.store(false, std::memory_order_relaxed);
            if (!m_running.load(std::memory_order_acquire)) break;
            std::this_thread::sleep_for(std::chrono::milliseconds(m_reconnectMs));
            continue;
        }

        RT_LOG_INFO("[NetworkTCI] Connected to %s:%u\n", host.c_str(), port);
        m_connected.store(true, std::memory_order_release);
        m_reconnectCount.fetch_add(1, std::memory_order_relaxed);

        workerLoop((int)sock);

        m_connected.store(false, std::memory_order_relaxed);
        RT_LOG_INFO("[NetworkTCI] Disconnected from %s:%u\n", host.c_str(), port);
        sockClose(sock);

        if (!m_running.load(std::memory_order_acquire)) break;
        std::this_thread::sleep_for(std::chrono::milliseconds(m_reconnectMs));
    }

    RT_LOG_INFO("[NetworkTCI] Client thread stopped\n");
}

// ─── Worker thread: server mode ───

void NetworkTCI::workerServer(uint16_t port) {
    RT_LOG_INFO("[NetworkTCI] Server thread starting on port %u\n", port);

    while (m_running.load(std::memory_order_acquire)) {
        SockHandle listenSock = INVALID_SOCK_HANDLE;
        if (!sockCreate(listenSock)) {
            RT_LOG_ERR("[NetworkTCI] Failed to create listen socket\n");
            std::this_thread::sleep_for(std::chrono::milliseconds(m_reconnectMs));
            continue;
        }

        int opt = 1;
#ifdef _WIN32
        setsockopt(listenSock, SOL_SOCKET, SO_REUSEADDR,
                   (const char*)&opt, sizeof(opt));
#else
        setsockopt(listenSock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
#endif

        struct sockaddr_in addr;
        std::memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_addr.s_addr = INADDR_ANY;
        addr.sin_port = htons(port);

        if (::bind(listenSock, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
            RT_LOG_ERR("[NetworkTCI] bind(port=%u) failed\n", port);
            sockClose(listenSock);
            std::this_thread::sleep_for(std::chrono::milliseconds(5000));
            continue;
        }

        if (::listen(listenSock, 1) < 0) {
            RT_LOG_ERR("[NetworkTCI] listen(port=%u) failed\n", port);
            sockClose(listenSock);
            std::this_thread::sleep_for(std::chrono::milliseconds(5000));
            continue;
        }

        RT_LOG_INFO("[NetworkTCI] Listening on port %u\n", port);

        struct sockaddr_in clientAddr;
#ifdef _WIN32
        int clientLen = sizeof(clientAddr);
#else
        socklen_t clientLen = sizeof(clientAddr);
#endif
        SockHandle clientSock = ::accept(listenSock, (struct sockaddr*)&clientAddr, &clientLen);
        sockClose(listenSock);

        if (clientSock == INVALID_SOCK_HANDLE) {
            RT_LOG_ERR("[NetworkTCI] accept() failed\n");
            continue;
        }

        char clientIP[64] = {};
        inet_ntop(AF_INET, &clientAddr.sin_addr, clientIP, sizeof(clientIP));
        RT_LOG_INFO("[NetworkTCI] Client connected from %s:%u\n",
                    clientIP, ntohs(clientAddr.sin_port));
        m_connected.store(true, std::memory_order_release);
        m_reconnectCount.fetch_add(1, std::memory_order_relaxed);

        workerLoop((int)clientSock);
        sockClose(clientSock);

        m_connected.store(false, std::memory_order_relaxed);
        RT_LOG_INFO("[NetworkTCI] Client disconnected\n");
    }

    RT_LOG_INFO("[NetworkTCI] Server thread stopped\n");
}

// ─── Common I/O loop (select-based, non-blocking) ───

void NetworkTCI::workerLoop(int fd) {
    SockHandle sock = (SockHandle)fd;
    sockSetNonBlocking(sock, true);

    while (m_running.load(std::memory_order_acquire)) {
        fd_set readfds, writefds;
        FD_ZERO(&readfds);
        FD_ZERO(&writefds);
        FD_SET(sock, &readfds);

        bool needWrite = false;
        {
            std::lock_guard<std::mutex> lock(m_mutex);
            needWrite = m_txPending;
        }
        if (needWrite) {
            FD_SET(sock, &writefds);
        }

        struct timeval tv;
        tv.tv_sec = 0;
        tv.tv_usec = 100000;  // 100ms timeout to check m_running

#ifdef _WIN32
        int ret = ::select(0, &readfds, &writefds, nullptr, &tv);
#else
        int ret = ::select(fd + 1, &readfds, &writefds, nullptr, &tv);
#endif

        if (ret < 0) break;

        if (ret == 0) continue;  // timeout, re-check m_running

        // ── Read ──
        if (FD_ISSET(sock, &readfds)) {
            uint8_t buf[4096];
#ifdef _WIN32
            int n = ::recv(sock, (char*)buf, sizeof(buf), 0);
#else
            int n = (int)::recv(sock, buf, sizeof(buf), 0);
#endif
            if (n <= 0) break;

            {
                std::lock_guard<std::mutex> lock(m_mutex);
                size_t copyLen = (size_t)n < m_rxBuf.size() ? (size_t)n : m_rxBuf.size();
                std::memcpy(m_rxBuf.data(), buf, copyLen);
                if ((size_t)n < m_rxBuf.size()) {
                    std::memset(m_rxBuf.data() + n, 0, m_rxBuf.size() - n);
                }
            }
            m_rxBytes.fetch_add((uint64_t)n, std::memory_order_relaxed);
        }

        // ── Write ──
        if (needWrite && FD_ISSET(sock, &writefds)) {
            bool hasData = false;
            uint8_t outBuf[NET_BUF_SIZE];
            int outLen = 0;

            {
                std::lock_guard<std::mutex> lock(m_mutex);
                if (m_txPending) {
                    int minOff = NET_BUF_SIZE;
                    int maxEnd = 0;
                    for (int i = 0; i < m_outputCount; i++) {
                        int s = m_outputMaps[i].netOffset;
                        int e = s + m_outputMaps[i].length;
                        if (s < minOff) minOff = s;
                        if (e > maxEnd) maxEnd = e;
                    }
                    if (maxEnd > minOff && maxEnd <= NET_BUF_SIZE) {
                        outLen = maxEnd - minOff;
                        std::memcpy(outBuf, m_txBuf.data() + minOff, (size_t)outLen);
                        hasData = true;
                    }
                    m_txPending = false;
                }
            }

            if (hasData) {
#ifdef _WIN32
                int sent = ::send(sock, (const char*)outBuf, outLen, 0);
#else
                int sent = (int)::send(sock, outBuf, (size_t)outLen, 0);
#endif
                if (sent < 0 && !isWouldBlock()) break;
                if (sent > 0) {
                    m_txBytes.fetch_add((uint64_t)sent, std::memory_order_relaxed);
                }
            }
        }
    }
}

} // namespace rt_plc
