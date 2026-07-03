#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <cctype>
#include <string>
#include <vector>
#include <map>
#include <thread>
#include <chrono>
#include <algorithm>

#ifdef _WIN32
#  include <winsock2.h>
#  include <ws2tcpip.h>
#  include <conio.h>
#  include <windows.h>
#  include <io.h>
#  pragma comment(lib, "ws2_32.lib")
#else
#  include <sys/socket.h>
#  include <netinet/in.h>
#  include <unistd.h>
#  include <arpa/inet.h>
#  include <sys/select.h>
#  include <termios.h>
#  include <fcntl.h>
#  define SOCKET int
#  define INVALID_SOCKET (-1)
#  define SOCKET_ERROR (-1)
#  define closesocket close
#endif

// ─── VarInfo: debug_map.json entry ───

struct VarInfo {
    int         id;
    std::string name;
    std::string storage;
    int         offset;
    int         bitOffset;
    std::string typeName;
    int         size;
    int         count;
    std::string access;
};

static std::vector<VarInfo> varInfos;
static std::map<int, int>   idToIndex;

static std::string trim(const std::string& s) {
    size_t a = 0, b = s.size();
    while (a < b && isspace((unsigned char)s[a])) a++;
    while (b > a && isspace((unsigned char)s[b-1])) b--;
    return s.substr(a, b-a);
}

#ifdef _WIN32
static bool sockInit() { WSADATA wsa; return WSAStartup(MAKEWORD(2,2),&wsa)==0; }
static void sockClean() { WSACleanup(); }
#else
static bool sockInit() { return true; }
static void sockClean() {}
#endif

// ─── Terminal input (non-blocking) ───

#ifdef _WIN32
static void enableAnsi() {
    HANDLE h = GetStdHandle(STD_OUTPUT_HANDLE);
    DWORD mode; GetConsoleMode(h, &mode);
    SetConsoleMode(h, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
}
static bool kbhitEx() { return _kbhit() != 0; }
static int  getchEx()  { return _getch(); }
static void termInit() { enableAnsi(); }
static void termRestore() {}
#else
static struct termios origTerm;
static void termInit() {
    tcgetattr(STDIN_FILENO, &origTerm);
    struct termios raw = origTerm;
    raw.c_lflag &= ~(ICANON | ECHO);
    tcsetattr(STDIN_FILENO, TCSANOW, &raw);
    int flags = fcntl(STDIN_FILENO, F_GETFL, 0);
    fcntl(STDIN_FILENO, F_SETFL, flags | O_NONBLOCK);
}
static void termRestore() {
    tcsetattr(STDIN_FILENO, TCSANOW, &origTerm);
}
static bool kbhitEx() {
    fd_set fds; FD_ZERO(&fds); FD_SET(STDIN_FILENO, &fds);
    timeval tv{0, 0};
    return select(STDIN_FILENO+1, &fds, nullptr, nullptr, &tv) > 0;
}
static int getchEx() {
    char c; return (read(STDIN_FILENO, &c, 1) == 1) ? (int)c : -1;
}
#endif

// ─── Minimal JSON parser for debug_map.json ───

static std::string jsonGetStr(const std::string& line, const char* key) {
    std::string pat = std::string("\"") + key + "\":";
    size_t p = line.find(pat);
    if (p == std::string::npos) return "";
    p += pat.size();
    while (p < line.size() && isspace((unsigned char)line[p])) p++;
    if (p >= line.size()) return "";
    if (line[p] == '"') {
        p++;
        size_t e = p;
        while (e < line.size() && line[e] != '"') {
            if (line[e] == '\\') e++;
            e++;
        }
        return line.substr(p, e - p);
    }
    size_t e = p;
    while (e < line.size() && (isdigit(line[e]) || line[e]=='-')) e++;
    return line.substr(p, e - p);
}

static int jsonGetInt(const std::string& line, const char* key, int def) {
    std::string v = jsonGetStr(line, key);
    if (v.empty()) return def;
    return atoi(v.c_str());
}

static bool loadDebugMap(const char* path) {
    FILE* f = fopen(path, "r");
    if (!f) { fprintf(stderr, "Cannot open %s\n", path); return false; }
    char buf[8192];
    std::string accum;
    while (fgets(buf, sizeof(buf), f)) accum += buf;
    fclose(f);

    size_t p = 0;
    while ((p = accum.find("\"id\":", p)) != std::string::npos) {
        size_t objStart = accum.rfind('{', p);
        if (objStart == std::string::npos) break;
        size_t objEnd = accum.find('}', p);
        if (objEnd == std::string::npos) break;
        std::string obj = accum.substr(objStart, objEnd - objStart + 1);

        VarInfo vi;
        vi.id        = jsonGetInt(obj, "id", -1);
        vi.name      = jsonGetStr(obj, "name");
        vi.storage   = jsonGetStr(obj, "storage");
        vi.offset    = jsonGetInt(obj, "offset", 0);
        vi.bitOffset = jsonGetInt(obj, "bit_offset", -1);
        vi.typeName  = jsonGetStr(obj, "type");
        vi.size      = jsonGetInt(obj, "size", 0);
        vi.count     = jsonGetInt(obj, "count", 1);
        vi.access    = jsonGetStr(obj, "access");
        if (vi.id < 0) { p = objEnd + 1; continue; }

        idToIndex[vi.id] = (int)varInfos.size();
        varInfos.push_back(vi);
        p = objEnd + 1;
    }
    printf("Loaded %zu variables from %s\n", varInfos.size(), path);
    return !varInfos.empty();
}

static const VarInfo* findById(int id) {
    auto it = idToIndex.find(id);
    return (it != idToIndex.end()) ? &varInfos[it->second] : nullptr;
}

static int findByName(const std::string& name) {
    for (auto& v : varInfos) if (v.name == name) return v.id;
    // 短名匹配：Counter → test_debug_full$MAIN$Counter
    for (auto& v : varInfos) {
        auto pos = v.name.rfind('$');
        std::string shortN = (pos != std::string::npos) ? v.name.substr(pos+1) : v.name;
        if (shortN == name) return v.id;
    }
    return -1;
}

// ─── TCP client ───

static SOCKET sock = INVALID_SOCKET;

static bool connectTcp(const char* host, int port) {
    if (!sockInit()) { fprintf(stderr, "WSAStartup failed\n"); return false; }
    sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock == INVALID_SOCKET) return false;
    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_port   = htons((uint16_t)port);
    addr.sin_addr.s_addr = inet_addr(host);
    if (connect(sock, (sockaddr*)&addr, sizeof(addr)) == SOCKET_ERROR) {
        fprintf(stderr, "Connect failed\n"); closesocket(sock); return false;
    }
    return true;
}

static void disconnectTcp() {
    if (sock != INVALID_SOCKET) { closesocket(sock); sock = INVALID_SOCKET; }
    sockClean();
}

static bool sendCmd(const std::string& cmd) {
    std::string c = cmd + "\n";
    return send(sock, c.c_str(), (int)c.size(), 0) > 0;
}

static std::string recvLine(int timeoutMs = 2000) {
    char buf[1];
    std::string r;
    auto start = std::chrono::steady_clock::now();
    while (true) {
        fd_set fds; FD_ZERO(&fds); FD_SET(sock, &fds);
        timeval tv{timeoutMs/1000, (timeoutMs%1000)*1000};
        int rc = select(0, &fds, nullptr, nullptr, &tv);
        if (rc <= 0) break;
        int n = recv(sock, buf, 1, 0);
        if (n <= 0) break;
        if (buf[0] == '\n') break;
        r += buf[0];
        auto now = std::chrono::steady_clock::now();
        if (std::chrono::duration_cast<std::chrono::milliseconds>(now-start).count() > timeoutMs) break;
    }
    return r;
}

static void drainRecv() {
    std::string l;
    while (!(l = recvLine(100)).empty()) {}
}

// ─── Value formatting ───

static std::string fmtValue(const VarInfo& vi, const std::string& hex) {
    if (hex.empty()) return "?";
    auto h2b = [](char c) -> uint8_t {
        if (c>='0'&&c<='9') return c-'0';
        if (c>='A'&&c<='F') return c-'A'+10;
        if (c>='a'&&c<='f') return c-'a'+10;
        return 0;
    };
    char buf[64];
    if (vi.typeName == "BOOL") {
        return (hex.size()>=2 && (hex[0]!='0'||hex[1]!='0')) ? "TRUE " : "FALSE";
    }
    if (vi.typeName == "REAL" || vi.typeName == "LREAL") {
        if (hex.size() < 4) return hex;
        union { uint32_t u; float f; } r; r.u = 0;
        for (size_t i=0;i<8&&i<hex.size();i+=2) r.u |= (uint32_t(h2b(hex[i]))<<4|h2b(hex[i+1])) << (i*4);
        snprintf(buf,sizeof(buf),"%.3f",(double)r.f); return buf;
    }
    if (vi.typeName == "INT" || vi.typeName == "DINT" || vi.typeName == "TIME" ||
        vi.typeName == "SINT" || vi.typeName == "LINT") {
        int64_t v = 0;
        for (size_t i=0;i<hex.size();i+=2)
            v |= (int64_t(h2b(hex[i]))<<4|h2b(hex[i+1])) << (i*4);
        snprintf(buf,sizeof(buf),"%lld",(long long)v); return buf;
    }
    return hex;
}

static std::string intToHex(int val, int minBytes = 1) {
    char hex[8]; int hl = 0;
    do { hex[hl++] = "0123456789ABCDEF"[val & 0xF]; val >>= 4; } while (val > 0);
    std::string hs;
    for (int i = hl-1; i >= 0; i--) hs += hex[i];
    if (hs.size() % 2) hs = "0" + hs;
    while ((int)hs.size() < minBytes * 2) hs = "0" + hs;
    return hs;
}

// ════════════════════════════════════════════════
// Monitor Mode — 实时面板
// ════════════════════════════════════════════════

struct WatchEntry {
    int         id;
    std::string hex;
    bool        forced;
};

struct MonitorState {
    std::vector<WatchEntry>  watchList;
    std::string              inputBuf;
    int                      refreshMs = 500;
    bool                     paused     = false;
    bool                     running    = true;
    uint64_t                 cycle      = 0;
    uint64_t                 totalTicks = 0;
    std::string              systemState;
    std::string              lastStatus;
};

static MonitorState mon;

static void sendWatchList() {
    std::string cmd = "WATCH";
    for (auto& w : mon.watchList) cmd += " " + std::to_string(w.id);
    if (mon.watchList.empty()) cmd += " 0";
    sendCmd(cmd);
    drainRecv();
}

static void parseReadResponse() {
    std::string header = recvLine(1000);
    if (header.empty()) return;

    // OK READ cycle=N time=N samples=N
    if (header.find("cycle=") != std::string::npos) {
        auto c1 = header.find("cycle=");
        auto c2 = header.find(" ", c1);
        if (c2 == std::string::npos) c2 = header.size();
        mon.cycle = strtoull(header.substr(c1+6, c2-c1-6).c_str(), nullptr, 10);
    }

    // 读 samples
    for (size_t i = 0; i < mon.watchList.size() + 2; i++) {
        std::string l = recvLine(300);
        if (l.empty()) break;
        if (l.find("OK") == 0) break;
        // sample id=N size=N hex=XXXX forced=N
        if (l.find("sample id=") != std::string::npos) {
            auto p1 = l.find("id=");
            auto p2 = l.find(" ", p1);
            if (p1 == std::string::npos) continue;
            int sid = atoi(l.substr(p1+3, (p2==std::string::npos?l.size():p2)-p1-3).c_str());
            auto h1 = l.find("hex=");
            auto h2 = l.find(" ", h1);
            if (h1 == std::string::npos) continue;
            std::string hex = (h2 != std::string::npos) ? l.substr(h1+4, h2-h1-4) : l.substr(h1+4);
            bool forced = (l.find("forced=1") != std::string::npos);
            for (auto& w : mon.watchList) {
                if (w.id == sid) { w.hex = hex; w.forced = forced; break; }
            }
        }
    }
}

static void parseDiagResponse() {
    std::string l = recvLine(500);
    if (l.empty()) return;
    // OK DIAG cycle=N totalTicks=N lastScan=N maxScan=N dropped=N
    auto p = l.find("totalTicks=");
    if (p != std::string::npos) {
        auto e = l.find(" ", p);
        if (e == std::string::npos) e = l.size();
        mon.totalTicks = strtoull(l.substr(p+11, e-p-11).c_str(), nullptr, 10);
    }
}

static std::string shortName(const std::string& fullName) {
    // test_debug_full$MAIN$Counter → Counter
    auto pos = fullName.rfind('$');
    return (pos != std::string::npos) ? fullName.substr(pos+1) : fullName;
}

static void drawTable() {
    // 光标归位（不清屏，避免闪烁）
    printf("\033[H");

    // 标题
    printf("\033[K\033[1;36m ST2C Debug Monitor\033[0m");
    printf("  cycle=\033[33m%llu\033[0m", (unsigned long long)mon.cycle);
    printf("  ticks=\033[33m%llu\033[0m", (unsigned long long)mon.totalTicks);
    printf("  refresh=%dms", mon.refreshMs);
    printf("  %s\033[0m\n", mon.paused ? "\033[31m[PAUSED]" : "");

    // 分隔线
    printf("\033[K\033[90m");
    for (int i = 0; i < 70; i++) printf("-");
    printf("\033[0m\n");

    // 表头
    printf("\033[K %-20s %-8s %-12s %-10s %s\n",
           "Variable", "Type", "Value", "Storage", "Forced");

    // 变量行
    if (mon.watchList.empty()) {
        printf("\033[K\n  \033[90m(no variables — use 'add <name>')\033[0m\n");
    } else {
        for (auto& w : mon.watchList) {
            const VarInfo* vi = findById(w.id);
            if (!vi) {
                printf("\033[K [%d] ???\n", w.id);
                continue;
            }
            std::string valStr;
            if (vi->count > 1) {
                valStr = fmtValue(*vi, w.hex) + " ...";
            } else {
                valStr = fmtValue(*vi, w.hex);
            }
            std::string forceMark = w.forced ? "\033[31;1m► FORCED\033[0m" : "";
            printf("\033[K %-20s \033[90m%-8s\033[0m \033[37m%-12s\033[0m \033[90m%-10s\033[0m %s\n",
                   shortName(vi->name).c_str(),
                   vi->typeName.c_str(),
                   valStr.c_str(),
                   vi->storage.c_str(),
                   forceMark.c_str());
        }
    }

    // 分隔线
    printf("\033[K\033[90m");
    for (int i = 0; i < 70; i++) printf("-");
    printf("\033[0m\n");

    // 状态行
    if (!mon.lastStatus.empty()) {
        printf("\033[K \033[33m%s\033[0m\n", mon.lastStatus.c_str());
    } else {
        printf("\033[K\n");
    }

    // 命令行
    printf("\033[K\n \033[1;32m>\033[0m %s\033[K", mon.inputBuf.c_str());
    fflush(stdout);
}

static void execMonitorCommand(const std::string& cmd) {
    mon.lastStatus = "";

    if (cmd.empty()) return;

    if (cmd == "quit" || cmd == "q") {
        mon.running = false;
        return;
    }
    if (cmd == "pause") {
        mon.paused = true;
        mon.lastStatus = "paused";
        return;
    }
    if (cmd == "resume") {
        mon.paused = false;
        mon.lastStatus = "resumed";
        return;
    }
    if (cmd.rfind("interval ", 0) == 0) {
        int ms = atoi(cmd.substr(9).c_str());
        if (ms >= 50) { mon.refreshMs = ms; mon.lastStatus = "interval=" + std::to_string(ms) + "ms"; }
        else mon.lastStatus = "min 50ms";
        return;
    }
    if (cmd.rfind("add ", 0) == 0) {
        std::string name = trim(cmd.substr(4));
        int id = findByName(name);
        if (id <= 0) { mon.lastStatus = "unknown: " + name; return; }
        for (auto& w : mon.watchList) if (w.id == id) { mon.lastStatus = "already watching"; return; }
        mon.watchList.push_back({id, "", false});
        sendWatchList();
        mon.lastStatus = "added " + shortName(name);
        return;
    }
    if (cmd.rfind("remove ", 0) == 0) {
        std::string name = trim(cmd.substr(7));
        int id = findByName(name);
        if (id <= 0) { mon.lastStatus = "unknown: " + name; return; }
        for (size_t i = 0; i < mon.watchList.size(); i++) {
            if (mon.watchList[i].id == id) {
                mon.watchList.erase(mon.watchList.begin() + i);
                sendWatchList();
                mon.lastStatus = "removed " + shortName(name);
                return;
            }
        }
        mon.lastStatus = "not in watch list";
        return;
    }
    if (cmd.rfind("force ", 0) == 0) {
        std::string args = trim(cmd.substr(6));
        auto sp = args.find(' ');
        if (sp == std::string::npos) { mon.lastStatus = "usage: force <name> <value>"; return; }
        std::string name = args.substr(0, sp);
        std::string val  = trim(args.substr(sp+1));
        int id = findByName(name);
        if (id <= 0) { mon.lastStatus = "unknown: " + name; return; }
        // 转换数值到 hex
        std::string hex;
        if (val == "true" || val == "TRUE" || val == "1") hex = "01";
        else if (val == "false" || val == "FALSE" || val == "0") hex = "00";
        else hex = intToHex(atoi(val.c_str()), 1);
        sendCmd("FORCE " + std::to_string(id) + " " + hex);
        drainRecv();
        mon.lastStatus = "forced " + shortName(name) + " = " + val;
        return;
    }
    if (cmd.rfind("unforce ", 0) == 0) {
        std::string name = trim(cmd.substr(8));
        int id = findByName(name);
        if (id <= 0) { mon.lastStatus = "unknown: " + name; return; }
        sendCmd("UNFORCE " + std::to_string(id));
        drainRecv();
        mon.lastStatus = "unforced " + shortName(name);
        return;
    }
    if (cmd == "clearforces") {
        sendCmd("CLEARFORCES");
        drainRecv();
        mon.lastStatus = "all forces cleared";
        return;
    }
    if (cmd == "help" || cmd == "h") {
        mon.lastStatus = "add/remove/force/unforce/clearforces/interval/pause/resume/quit";
        return;
    }

    mon.lastStatus = "unknown command: " + cmd + " (try help)";
}

static void monitorMode() {
    termInit();

    // 检测 stdin 是否为交互终端
#ifdef _WIN32
    bool isTTY = _isatty(_fileno(stdin)) != 0;
#else
    bool isTTY = isatty(STDIN_FILENO) != 0;
#endif

    // 清屏一次
    printf("\033[2J");

    // 自动加所有标量变量到 watch list
    for (auto& v : varInfos) {
        if (v.count == 1) {
            mon.watchList.push_back({v.id, "", false});
        }
    }
    sendWatchList();
    drainRecv();

    // 等 2 个周期让 snapshot 生成
#ifdef _WIN32
    Sleep(300);
#else
    usleep(300000);
#endif

    // 先发一次 DIAG 获取 totalTicks
    sendCmd("DIAG");
    parseDiagResponse();

    mon.running = true;
    auto lastRefresh = std::chrono::steady_clock::now();

    while (mon.running) {
        // 检查键盘输入
        if (isTTY) {
            // 交互终端：用 kbhit/getch 非阻塞读
            while (kbhitEx()) {
                int ch = getchEx();
                if (ch == '\r' || ch == '\n') {
                    std::string cmd = trim(mon.inputBuf);
                    mon.inputBuf.clear();
                    execMonitorCommand(cmd);
                    if (!mon.running) goto done;
                } else if (ch == 8 || ch == 127) {
                    if (!mon.inputBuf.empty()) mon.inputBuf.pop_back();
                } else if (ch == 3) {
                    mon.running = false;
                    goto done;
                } else if (ch >= 32 && ch < 127) {
                    mon.inputBuf += (char)ch;
                }
            }
        } else {
            // 管道模式：用 select 超时检查 stdin
            fd_set fds; FD_ZERO(&fds);
#ifdef _WIN32
            FD_SET(_fileno(stdin), &fds);
            timeval tv{0, 0};
            if (select(0, &fds, nullptr, nullptr, &tv) > 0) {
#else
            FD_SET(STDIN_FILENO, &fds);
            timeval tv{0, 0};
            if (select(STDIN_FILENO+1, &fds, nullptr, nullptr, &tv) > 0) {
#endif
                char buf[256];
                if (fgets(buf, sizeof(buf), stdin)) {
                    std::string cmd = trim(buf);
                    execMonitorCommand(cmd);
                    if (!mon.running) goto done;
                } else {
                    // stdin EOF
                    mon.running = false;
                    goto done;
                }
            }
        }

        // 定时刷新
        auto now = std::chrono::steady_clock::now();
        auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - lastRefresh).count();
        if (!mon.paused && elapsed >= mon.refreshMs) {
            lastRefresh = now;
            sendCmd("READ");
            parseReadResponse();
            sendCmd("DIAG");
            parseDiagResponse();
        }

        drawTable();

        // 短暂等待（避免 CPU 100%）
#ifdef _WIN32
        Sleep(20);
#else
        usleep(20000);
#endif
    }

done:
    termRestore();
    printf("\033[2J\033[H");
    printf("Exited monitor mode.\n");
}

// ─── Original Interactive loop ───

static void interactiveLoop() {
    char line[1024];
    printf("\nST2C Debug CLI — type 'help' for commands\n");
    while (true) {
        printf("> "); fflush(stdout);
        if (!fgets(line, sizeof(line), stdin)) break;
        std::string cmd = trim(line);
        if (cmd.empty()) continue;

        if (cmd == "help") {
            printf("  hello            — server greeting\n");
            printf("  list             — list all variables\n");
            printf("  watch <name>...  — set watch list\n");
            printf("  read             — read latest snapshot\n");
            printf("  monitor          — enter live monitor mode (real-time panel)\n");
            printf("  force <name> <val> — force a variable\n");
            printf("  unforce <name>   — clear force\n");
            printf("  diag             — diagnostics\n");
            printf("  quit             — disconnect\n");
        }
        else if (cmd == "monitor") {
            monitorMode();
        }
        else if (cmd == "hello") {
            sendCmd("HELLO");
            printf("  %s\n", recvLine().c_str());
        }
        else if (cmd == "list") {
            sendCmd("LIST");
            for (int i = 0; i < (int)varInfos.size(); i++) recvLine();
            printf("  display: %zu variables from debug_map\n", varInfos.size());
        }
        else if (cmd.rfind("watch ",0) == 0) {
            std::string args = cmd.substr(6);
            std::vector<int> ids;
            size_t p = 0;
            while (p < args.size()) {
                while (p < args.size() && isspace((unsigned char)args[p])) p++;
                size_t e = p;
                while (e < args.size() && !isspace((unsigned char)args[e])) e++;
                if (e > p) {
                    std::string name = args.substr(p, e - p);
                    int id = findByName(name);
                    if (id > 0) ids.push_back(id);
                    else printf("  unknown: %s\n", name.c_str());
                }
                p = e;
            }
            if (!ids.empty()) {
                std::string watchCmd = "WATCH";
                for (int id : ids) { watchCmd += " " + std::to_string(id); }
                sendCmd(watchCmd);
                recvLine();
                printf("  watching %zu variables\n", ids.size());
            }
        }
        else if (cmd == "read") {
            sendCmd("READ");
            std::string header = recvLine();
            printf("  %s\n", header.c_str());
            for (int i = 0; i < (int)varInfos.size(); i++) {
                std::string l = recvLine(500);
                if (l.empty() || l.find("OK") == 0) break;
                auto p1 = l.find("id=");
                auto p2 = l.find(" ", p1);
                if (p1 == std::string::npos || p2 == std::string::npos) continue;
                int sid = atoi(l.substr(p1+3, p2-p1-3).c_str());
                auto h1 = l.find("hex=");
                auto h2 = l.find(" ", h1);
                if (h1 == std::string::npos) continue;
                std::string hex = (h2 != std::string::npos) ? l.substr(h1+4, h2-h1-4) : l.substr(h1+4);
                auto fi = idToIndex.find(sid);
                const VarInfo* vi = (fi != idToIndex.end()) ? &varInfos[fi->second] : nullptr;
                auto f1 = l.find("forced=");
                bool forced = f1 != std::string::npos && l[f1+7] == '1';
                if (vi) {
                    printf("  [%d] %-20s = %-12s (%s) %s\n",
                           sid, vi->name.c_str(), fmtValue(*vi, hex).c_str(),
                           vi->typeName.c_str(), forced ? "[FORCED]" : "");
                } else {
                    printf("  [%d] hex=%s\n", sid, hex.c_str());
                }
            }
        }
        else if (cmd.rfind("force ",0)==0) {
            auto sp = [&](const std::string& s, size_t& p) -> std::string {
                while (p<s.size()&&isspace((unsigned char)s[p]))p++;
                size_t e=p; while(e<s.size()&&!isspace((unsigned char)s[e]))e++;
                return s.substr(p,(e>p)?(e-p):0);
            };
            size_t p1 = 6;
            std::string name = sp(cmd, p1);
            std::string val  = sp(cmd, p1);
            int id = findByName(name);
            if (id <= 0) { printf("  unknown: %s\n", name.c_str()); continue; }
            std::string hex;
            if (val == "true" || val == "TRUE" || val == "1") hex = "01";
            else if (val == "false" || val == "FALSE" || val == "0") hex = "00";
            else hex = intToHex(atoi(val.c_str()), 1);
            sendCmd("FORCE " + std::to_string(id) + " " + hex);
            printf("  %s\n", recvLine().c_str());
        }
        else if (cmd.rfind("unforce ",0) == 0) {
            int id = findByName(cmd.substr(8));
            if (id <= 0) { printf("  unknown\n"); continue; }
            sendCmd("UNFORCE " + std::to_string(id));
            printf("  %s\n", recvLine().c_str());
        }
        else if (cmd == "diag") {
            sendCmd("DIAG");
            printf("  %s\n", recvLine().c_str());
        }
        else if (cmd == "quit") {
            sendCmd("QUIT"); recvLine(); break;
        }
        else {
            printf("  unknown command. type 'help'\n");
        }
    }
}

int main(int argc, char* argv[]) {
    if (argc < 3) {
        printf("Usage: debug_cli <host> <port> <debug_map.json>\n");
        printf("   or: debug_cli <host> <port>           (list mode)\n");
        printf("   or: debug_cli <host> <port> <json> monitor  (live monitor)\n");
        return 1;
    }
    const char* host = argv[1];
    int  port        = atoi(argv[2]);
    const char* json = (argc >= 4) ? argv[3] : nullptr;

    if (json) {
        if (!loadDebugMap(json)) return 1;
    } else {
        printf("no debug_map.json — raw ID mode\n");
    }

    if (!connectTcp(host, port)) {
        fprintf(stderr, "Failed to connect to %s:%d\n", host, port);
        return 1;
    }
    printf("Connected to %s:%d\n", host, port);

    // 跳过 greeting
    recvLine(1000);

    // 如果第四个参数是 monitor，直接进面板
    if (argc >= 5 && strcmp(argv[4], "monitor") == 0) {
        monitorMode();
    } else {
        interactiveLoop();
    }

    disconnectTcp();
    printf("Disconnected.\n");
    return 0;
}
