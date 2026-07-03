#include "debug_tcp_server.h"

#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <thread>
#include <atomic>
#include <vector>
#include <string>
#include <cctype>

#ifdef _WIN32
#  include <winsock2.h>
#  include <ws2tcpip.h>
#  pragma comment(lib, "ws2_32.lib")
#else
#  include <sys/socket.h>
#  include <netinet/in.h>
#  include <unistd.h>
#  include <arpa/inet.h>
#  define SOCKET int
#  define INVALID_SOCKET (-1)
#  define SOCKET_ERROR (-1)
#  define closesocket close
#endif

namespace rt_plc {
namespace {

static std::thread      serverThread_;
static std::atomic<bool> running_{false};
static int              listenPort_ = 0;

// ── helpers ──

#ifdef _WIN32
static bool socketInit() {
    WSADATA wsa;
    return WSAStartup(MAKEWORD(2, 2), &wsa) == 0;
}
static void socketCleanup() { WSACleanup(); }
#else
static bool socketInit() { return true; }
static void socketCleanup() {}
#endif

static std::string trim(const std::string& s) {
    size_t a = 0, b = s.size();
    while (a < b && std::isspace(static_cast<unsigned char>(s[a]))) a++;
    while (b > a && std::isspace(static_cast<unsigned char>(s[b-1]))) b--;
    return s.substr(a, b - a);
}

static std::vector<std::string> split(const std::string& s, char delim) {
    std::vector<std::string> out;
    size_t p = 0;
    while (p < s.size()) {
        size_t q = s.find(delim, p);
        if (q == std::string::npos) q = s.size();
        out.push_back(s.substr(p, q - p));
        p = q + 1;
    }
    return out;
}

static std::string toHex(const uint8_t* data, size_t len) {
    if (len == 0) return "00";
    static const char hex[] = "0123456789ABCDEF";
    std::string r;
    for (size_t i = 0; i < len; i++) {
        r += hex[data[i] >> 4];
        r += hex[data[i] & 0x0F];
    }
    return r;
}

static bool fromHex(const std::string& s, uint8_t* out, size_t maxLen, size_t& outLen) {
    size_t n = s.size() / 2;
    if (n > maxLen) return false;
    for (size_t i = 0; i < n; i++) {
        char h = s[i*2], l = s[i*2+1];
        int vh = (h >= '0' && h <= '9') ? (h - '0') : (h >= 'A' && h <= 'F') ? (h - 'A' + 10) :
                 (h >= 'a' && h <= 'f') ? (h - 'a' + 10) : -1;
        int vl = (l >= '0' && l <= '9') ? (l - '0') : (l >= 'A' && l <= 'F') ? (l - 'A' + 10) :
                 (l >= 'a' && l <= 'f') ? (l - 'a' + 10) : -1;
        if (vh < 0 || vl < 0) return false;
        out[i] = static_cast<uint8_t>((vh << 4) | vl);
    }
    outLen = n;
    return true;
}

static void sendStr(SOCKET sock, const std::string& msg) {
    ::send(sock, msg.c_str(), static_cast<int>(msg.size()), 0);
}

static std::string storageName(DebugStorage s) {
    switch (s) {
        case DebugStorage::GVL:    return "GVL";
        case DebugStorage::INPUT:  return "INPUT";
        case DebugStorage::OUTPUT: return "OUTPUT";
        case DebugStorage::MEMORY: return "MEMORY";
        case DebugStorage::RETAIN: return "RETAIN";
    }
    return "GVL";
}

static DebugStorage parseStorage(const std::string& s) {
    if (s == "GVL")    return DebugStorage::GVL;
    if (s == "INPUT")  return DebugStorage::INPUT;
    if (s == "OUTPUT") return DebugStorage::OUTPUT;
    if (s == "MEMORY") return DebugStorage::MEMORY;
    if (s == "RETAIN") return DebugStorage::RETAIN;
    return DebugStorage::GVL;
}

// ── session ──

static void handleSession(SOCKET client, DebugEngine& engine) {
    char buf[4096];
    std::string accum;

    sendStr(client, "OK ST2C Debug Server\n");

    while (running_) {
        int n = recv(client, buf, sizeof(buf) - 1, 0);
        if (n <= 0) break;
        buf[n] = 0;
        accum += buf;

        size_t nl;
        while ((nl = accum.find('\n')) != std::string::npos) {
            std::string line = trim(accum.substr(0, nl));
            accum.erase(0, nl + 1);
            if (line.empty()) continue;

            auto parts = split(line, ' ');
            if (parts.empty()) continue;
            std::string cmd = parts[0];

            if (cmd == "HELLO" || cmd == "hello") {
                uint8_t bid[16];
                uint32_t vc;
                engine.hello(bid, vc);
                char r[128];
                snprintf(r, sizeof(r), "OK HELLO protocol=1 build_id=%s var_count=%u\n",
                         toHex(bid, 16).c_str(), vc);
                sendStr(client, r);
            }
            else if (cmd == "LIST" || cmd == "list") {
                uint32_t vc = engine.varCount();
                const DebugVar* vars = engine.vars();
                for (uint32_t i = 0; i < vc; i++, vars++) {
                    char r[256];
                    snprintf(r, sizeof(r),
                        "var id=%u storage=%s offset=%u bit=%u type=%d size=%u count=%u access=%d\n",
                        vars->id, storageName(vars->storage).c_str(),
                        vars->offset, vars->bitOffset,
                        static_cast<int>(vars->type), vars->size, vars->count,
                        static_cast<int>(vars->access));
                    sendStr(client, r);
                }
                sendStr(client, "OK LIST\n");
            }
            else if (cmd == "WATCH" || cmd == "watch") {
                uint32_t ids[64];
                uint32_t cnt = 0;
                for (size_t i = 1; i < parts.size() && cnt < 64; i++) {
                    ids[cnt++] = static_cast<uint32_t>(std::atoi(parts[i].c_str()));
                }
                engine.requestSetWatchList(ids, cnt);
                sendStr(client, "OK WATCH\n");
            }
            else if (cmd == "READ" || cmd == "read") {
                const DebugSnapshot& snap = engine.latestSnapshot();
                char r[256];
                snprintf(r, sizeof(r), "OK READ cycle=%llu time=%lld samples=%u\n",
                         (unsigned long long)snap.cycle, (long long)snap.systemTime,
                         snap.sampleCount);
                sendStr(client, r);
                for (uint32_t i = 0; i < snap.sampleCount; i++) {
                    const DebugSample& s = snap.samples[i];
                    char v[128];
                    snprintf(v, sizeof(v), "sample id=%u size=%u hex=%s forced=%d\n",
                             s.id, s.size, toHex(s.value, s.size).c_str(),
                             engine.isForced(s.id) ? 1 : 0);
                    sendStr(client, v);
                }
            }
            else if (cmd == "RANGE" || cmd == "range") {
                if (parts.size() < 4) {
                    sendStr(client, "ERR BAD_ARGS\n"); continue;
                }
                DebugStorage st = parseStorage(parts[1]);
                uint32_t off = static_cast<uint32_t>(std::atoi(parts[2].c_str()));
                uint32_t sz = static_cast<uint32_t>(std::atoi(parts[3].c_str()));
                if (sz > 4096) sz = 4096;
                uint8_t data[4096];
                if (engine.readMemory(st, off, sz, data)) {
                    char r[64];
                    snprintf(r, sizeof(r), "OK RANGE %s %u %u hex=",
                             storageName(st).c_str(), off, sz);
                    sendStr(client, r);
                    sendStr(client, toHex(data, sz) + "\n");
                } else {
                    sendStr(client, "ERR RANGE_FAILED\n");
                }
            }
            else if (cmd == "FORCE" || cmd == "force") {
                if (parts.size() < 3) {
                    sendStr(client, "ERR BAD_ARGS\n"); continue;
                }
                uint32_t id = static_cast<uint32_t>(std::atoi(parts[1].c_str()));
                uint8_t val[8];
                size_t valLen;
                if (!fromHex(parts[2], val, 8, valLen)) {
                    sendStr(client, "ERR HEX_PARSE\n"); continue;
                }
                if (engine.requestForce(id, val, static_cast<uint16_t>(valLen)))
                    sendStr(client, "OK FORCE\n");
                else
                    sendStr(client, "ERR FORCE_FAILED\n");
            }
            else if (cmd == "UNFORCE" || cmd == "unforce") {
                if (parts.size() < 2) {
                    sendStr(client, "ERR BAD_ARGS\n"); continue;
                }
                uint32_t id = static_cast<uint32_t>(std::atoi(parts[1].c_str()));
                if (engine.requestUnforce(id))
                    sendStr(client, "OK UNFORCE\n");
                else
                    sendStr(client, "ERR UNFORCE_FAILED\n");
            }
            else if (cmd == "CLEARFORCES" || cmd == "clearforces") {
                engine.requestClearForces();
                sendStr(client, "OK CLEARFORCES\n");
            }
            else if (cmd == "DIAG" || cmd == "diag") {
                const DebugSnapshot& snap = engine.latestSnapshot();
                char r[256];
                snprintf(r, sizeof(r),
                    "OK DIAG cycle=%llu totalTicks=%llu lastScan=%lld maxScan=%lld dropped=%u\n",
                    (unsigned long long)snap.cycle,
                    (unsigned long long)snap.totalTicks,
                    (long long)snap.lastScanTime,
                    (long long)snap.maxScanTime,
                    engine.droppedCommands());
                sendStr(client, r);
            }
            else if (cmd == "QUIT" || cmd == "quit") {
                sendStr(client, "OK BYE\n");
                goto done;
            }
            else {
                sendStr(client, "ERR UNKNOWN_CMD\n");
            }
        }
    }
done:
    closesocket(client);
}

// ── server thread ──

static void serverLoop(DebugEngine& engine, int port) {
    if (!socketInit()) {
        std::fprintf(stderr, "[DebugServer] socket init failed\n");
        running_ = false;
        return;
    }

    SOCKET listenSock = socket(AF_INET, SOCK_STREAM, 0);
    if (listenSock == INVALID_SOCKET) {
        std::fprintf(stderr, "[DebugServer] socket() failed\n");
        running_ = false;
        socketCleanup();
        return;
    }

    int opt = 1;
    setsockopt(listenSock, SOL_SOCKET, SO_REUSEADDR,
#if defined(_WIN32)
               reinterpret_cast<const char*>(&opt),
#else
               &opt,
#endif
               sizeof(opt));

    sockaddr_in addr{};
    addr.sin_family      = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port        = htons(static_cast<uint16_t>(port));

    if (::bind(listenSock, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) == SOCKET_ERROR) {
        std::fprintf(stderr, "[DebugServer] bind(:%d) failed\n", port);
        closesocket(listenSock);
        running_ = false;
        socketCleanup();
        return;
    }

    if (::listen(listenSock, 1) == SOCKET_ERROR) {
        std::fprintf(stderr, "[DebugServer] listen() failed\n");
        closesocket(listenSock);
        running_ = false;
        socketCleanup();
        return;
    }

    std::printf("[DebugServer] listening on 127.0.0.1:%d\n", port);

    while (running_) {
        sockaddr_in clientAddr{};
        socklen_t clen = sizeof(clientAddr);
        SOCKET client = accept(listenSock, reinterpret_cast<sockaddr*>(&clientAddr), &clen);
        if (client == INVALID_SOCKET) {
            if (running_) continue;
            break;
        }
        handleSession(client, engine);
    }

    closesocket(listenSock);
    socketCleanup();
}

} // anonymous namespace

bool debugTcpServerStart(DebugEngine& engine, int port) {
    if (running_) return false;
    running_ = true;
    listenPort_ = port;
    serverThread_ = std::thread(serverLoop, std::ref(engine), port);
    return true;
}

void debugTcpServerStop() {
    running_ = false;
    if (serverThread_.joinable()) serverThread_.join();
}

bool debugTcpServerRunning() {
    return running_;
}

} // namespace rt_plc
