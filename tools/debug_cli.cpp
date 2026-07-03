#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <cctype>
#include <string>
#include <vector>
#include <map>
#include <thread>
#include <chrono>

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

// ─── Value formatting ───

static std::string fmtValue(const VarInfo& vi, const std::string& hex) {
    if (hex.empty() || hex == "00") return "0";
    auto h2b = [](char c) -> uint8_t {
        if (c>='0'&&c<='9') return c-'0';
        if (c>='A'&&c<='F') return c-'A'+10;
        if (c>='a'&&c<='f') return c-'a'+10;
        return 0;
    };
    char buf[64];
    if (vi.typeName == "BOOL") {
        return (hex[0]!='0'||hex[1]!='0') ? "TRUE" : "FALSE";
    }
    if (vi.typeName == "REAL" || vi.typeName == "LREAL") {
        if (hex.size() < 4) return hex;
        union { uint32_t u; float f; } r; r.u = 0;
        for (size_t i=0;i<8&&i<hex.size();i+=2) r.u |= (uint32_t(h2b(hex[i]))<<4|h2b(hex[i+1])) << (i*4);
        snprintf(buf,sizeof(buf),"%f",(double)r.f); return buf;
    }
    if (vi.typeName == "INT" || vi.typeName == "DINT" || vi.typeName == "TIME") {
        int64_t v = 0;
        for (size_t i=0;i<hex.size();i+=2)
            v |= (int64_t(h2b(hex[i]))<<4|h2b(hex[i+1])) << (i*4);
        snprintf(buf,sizeof(buf),"%lld",(long long)v); return buf;
    }
    return hex;
}

// ─── Interactive loop ───

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
            printf("  force <name> <val> — force a variable\n");
            printf("  unforce <name>   — clear force\n");
            printf("  diag             — diagnostics\n");
            printf("  quit             — disconnect\n");
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
                // parse: sample id=X size=X hex=XXXXXX forced=X
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
            int iv = atoi(val.c_str());
            char hex[8]; int hl=0;
            do { hex[hl++]="0123456789ABCDEF"[iv&0xF]; iv>>=4; } while(iv>0);
            std::string hs;
            for(int i=hl-1;i>=0;i--) hs+=hex[i];
            if (hs.size()%2) hs="0"+hs;
            sendCmd("FORCE " + std::to_string(id) + " " + hs);
            printf("  %s\n", recvLine().c_str());
        }
        else if (cmd.rfind("unforce ",0)==0) {
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

    interactiveLoop();

    disconnectTcp();
    printf("Disconnected.\n");
    return 0;
}
