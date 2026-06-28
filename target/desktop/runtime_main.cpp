/**
 * runtime_main.cpp -- ST2C++ Flat Runtime (generic)
 *
 * Responsibilities:
 *   1. Read tasks.json (user/editor) defining Task <-> POU mapping
 *   2. Look up compiled POUs from POURegistry
 *   3. Register POUs into Scheduler per configuration
 *   4. Run scheduling loop + diagnostics
 *
 * No external JSON library (hand-written parser, minimum subset).
 * Compiler only registers POUs (metadata): POURegistry::add()
 * Runtime fully owns scheduling policy and configuration loading.
 */
#include "rt_runtime.h"
#if defined(RT_ETHERCAT_ENABLED)
#include "ethercat_tci.h"
#endif
#include <cstdio>
#include <string>
#include <vector>
#include <fstream>
#include <sstream>
#include "core/platform.h"
#include <csignal>

using namespace rt_plc;

static volatile bool running = true;
static void signalHandler(int) { running = false; }

// ═══════════════════════════════════════════════════════
//  Minimal JSON Parser (tasks.json only)
// ═══════════════════════════════════════════════════════
namespace json_cfg {

struct JsonValue {
    enum Type { NULL_VAL, INT_VAL, STRING_VAL, ARRAY_VAL, OBJECT_VAL };
    Type type = NULL_VAL;
    int64_t intVal = 0;
    std::string strVal;
    std::vector<JsonValue> arrVal;
    std::vector<std::pair<std::string, JsonValue>> objVal;

    const JsonValue& get(const char* key) const {
        for (auto& [k, v] : objVal)
            if (k == key) return v;
        static JsonValue null;
        return null;
    }
    const JsonValue& operator[](size_t i) const { return arrVal[i]; }
    size_t size() const { return arrVal.size(); }
    explicit operator int() const { return (int)intVal; }
    const char* c_str() const { return strVal.c_str(); }
};

struct JsonParser {
    const char* p;

    void skipWS() {
        while (*p && (*p == ' ' || *p == '\t' || *p == '\n' || *p == '\r')) p++;
    }

    JsonValue parseValue() {
        skipWS();
        if (*p == '"') return parseString();
        if (*p == '{') return parseObject();
        if (*p == '[') return parseArray();
        return parseNumber();
    }

    JsonValue parseString() {
        JsonValue v;
        v.type = JsonValue::STRING_VAL;
        p++;
        while (*p && *p != '"') {
            if (*p == '\\') { p++; if (*p) { v.strVal += *p; p++; } }
            else { v.strVal += *p; p++; }
        }
        if (*p == '"') p++;
        return v;
    }

    JsonValue parseNumber() {
        JsonValue v;
        v.type = JsonValue::INT_VAL;
        int64_t sign = 1;
        if (*p == '-') { sign = -1; p++; }
        while (*p && *p >= '0' && *p <= '9') {
            v.intVal = v.intVal * 10 + (*p - '0');
            p++;
        }
        v.intVal *= sign;
        return v;
    }

    JsonValue parseArray() {
        JsonValue v;
        v.type = JsonValue::ARRAY_VAL;
        p++;
        skipWS();
        if (*p == ']') { p++; return v; }
        while (true) {
            v.arrVal.push_back(parseValue());
            skipWS();
            if (*p == ']') { p++; return v; }
            if (*p == ',') p++;
            skipWS();
        }
    }

    JsonValue parseObject() {
        JsonValue v;
        v.type = JsonValue::OBJECT_VAL;
        p++;
        skipWS();
        if (*p == '}') { p++; return v; }
        while (true) {
            skipWS();
            auto key = parseString();
            skipWS();
            if (*p == ':') p++;
            skipWS();
            v.objVal.push_back({key.strVal, parseValue()});
            skipWS();
            if (*p == '}') { p++; return v; }
            if (*p == ',') p++;
        }
    }

    JsonValue parse(const std::string& text) {
        p = text.c_str();
        return parseValue();
    }
};

// ═══════════════════════════════════════════════════════
//  Configuration data structures
// ═══════════════════════════════════════════════════════

struct TaskConfig {
    std::string name;
    int priority   = 5;
    int intervalUs = 10000;
    int watchdogUs = 5000;
    std::vector<std::string> pous;
};

struct Config {
    int baseCycleUs = 1000;
    std::vector<TaskConfig> tasks;

    static Config loadOrDefault(const char* path) {
        Config cfg;
        std::ifstream file(path);
        if (!file.is_open()) {
            RT_LOG_INFO("  [runtime] tasks.json not found, using defaults\n");
            return cfg;
        }
        std::stringstream buf;
        buf << file.rdbuf();
        file.close();

        JsonParser parser;
        JsonValue root = parser.parse(buf.str());

        if (root.type == JsonValue::OBJECT_VAL) {
            if (root.get("base_cycle_us").type != JsonValue::NULL_VAL)
                cfg.baseCycleUs = (int)root.get("base_cycle_us");

            const auto& tasks = root.get("tasks");
            for (size_t i = 0; i < tasks.size(); i++) {
                TaskConfig tc;
                const auto& t = tasks[i];
                tc.name       = t.get("name").c_str();
                tc.priority   = (int)t.get("priority");
                tc.intervalUs = (int)t.get("interval_us");
                tc.watchdogUs = (int)t.get("watchdog_us");
                const auto& pous = t.get("pous");
                for (size_t j = 0; j < pous.size(); j++)
                    tc.pous.push_back(pous[j].c_str());
                cfg.tasks.push_back(tc);
            }
        }
        return cfg;
    }
};

} // namespace json_cfg

// ═══════════════════════════════════════════════════════
//  POU Registration (generated by build script)
// ═══════════════════════════════════════════════════════
// Defined in pou_registry.gen.cpp — aggregates all registerPOU_<fileId>() calls
// When building runtime_main standalone (without generated POU code), use the stub below
#ifndef RUNTIME_MAIN_STANDALONE
extern void registerAllPOUs(POURegistry& reg);
#else
void registerAllPOUs(POURegistry& reg) {}
#endif

// ═══════════════════════════════════════════════════════
//  Jitter measurement
// ═══════════════════════════════════════════════════════

static struct {
    int64_t intervalUs;
    int64_t prevUs;
    int64_t minJitter, maxJitter;
    int64_t sumJitter;
    int64_t sumSquareJitter;
    uint64_t count;
    bool ready;
} jitter = {1000, 0, 999999, -999999, 0, 0, 0, false};

static void jitterReset(int64_t us) {
    jitter.intervalUs = us;
    jitter.prevUs = 0; jitter.minJitter = 999999; jitter.maxJitter = -999999;
    jitter.sumJitter = 0; jitter.sumSquareJitter = 0; jitter.count = 0; jitter.ready = false;
}

static void jitterSample(int64_t now) {
    if (!jitter.ready) { jitter.prevUs = now; jitter.ready = true; return; }
    int64_t actual = now - jitter.prevUs;
    int64_t jit = actual - jitter.intervalUs;
    jitter.prevUs = now;
    jitter.minJitter = std::min(jitter.minJitter, jit);
    jitter.maxJitter = std::max(jitter.maxJitter, jit);
    jitter.sumJitter += jit;
    jitter.sumSquareJitter += jit * jit;
    jitter.count++;
}

static void printJitterStats() {
    if (jitter.count > 0) {
        int64_t avg = jitter.sumJitter / (int64_t)jitter.count;
        double variance = jitter.count > 1
            ? (double)jitter.sumSquareJitter / jitter.count - (double)avg * avg
            : 0;
        printf("\n=== Jitter Stats ===\n");
        printf("  Target  : %lld us\n", (long long)jitter.intervalUs);
        printf("  Samples : %llu\n", (unsigned long long)jitter.count);
        printf("  Min     : %lld us\n", (long long)jitter.minJitter);
        printf("  Max     : %lld us\n", (long long)jitter.maxJitter);
        printf("  Avg     : %lld us\n", (long long)avg);
        printf("  StdDev  : %lld us\n", (long long)(variance > 0 ? (int64_t)std::sqrt(variance) : 0));
    }
}

// ═══════════════════════════════════════════════════════
//  Main
// ═══════════════════════════════════════════════════════

int main(int argc, char* argv[]) {
    RT_LOG_INFO("=== ST2C++ Flat Runtime ===\n\n");

    int64_t cycleUs = 1000;
    int diagIntervalS = 5;
    bool jitterOnly = false;

    // TCI 配置
    enum class TciMode { NONE, ETHERCAT };
    TciMode tciMode = TciMode::NONE;
    char ecatIfname[32] = "eth0";
#if !defined(RT_ETHERCAT_ENABLED)
    (void)tciMode; (void)ecatIfname;
#endif

    for (int i = 1; i < argc; i++) {
        if (argv[i][0] != '-') continue;
        if (strcmp(argv[i], "--cycle-us") == 0 && i + 1 < argc) {
            cycleUs = atol(argv[++i]);
        } else if (strcmp(argv[i], "--diag-interval") == 0 && i + 1 < argc) {
            diagIntervalS = atoi(argv[++i]);
        } else if (strcmp(argv[i], "--jitter-only") == 0) {
            jitterOnly = true;
        } else if (strcmp(argv[i], "--tci") == 0 && i + 1 < argc) {
            i++;
            if (strcmp(argv[i], "ethercat") == 0) tciMode = TciMode::ETHERCAT;
            else fprintf(stderr, "Unknown TCI mode: %s (use ethercat)\n", argv[i]);
        } else if (strcmp(argv[i], "--ecat-if") == 0 && i + 1 < argc) {
            strncpy(ecatIfname, argv[i + 1], sizeof(ecatIfname) - 1);
            i++;
        } else if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            printf("Usage: %s [options] [tasks.json]\n", argv[0]);
            printf("  --cycle-us <us>       PLC 周期（微秒，默认 1000）\n");
            printf("  --diag-interval <s>   诊断间隔（秒，默认 5）\n");
            printf("  --jitter-only         纯定时器抖动测试\n");
            printf("  --tci <mode>          I/O 模式: ethercat\n");
            printf("  --ecat-if <name>      EtherCAT 网卡接口 (默认 eth0)\n");
            printf("\n注意：桌面平台计时精度依赖 OS，1ms 周期抖动 ~100-500us\n");
            return 0;
        }
    }

    // Ctrl+C / SIGINT
    std::signal(SIGINT, signalHandler);
    std::signal(SIGTERM, signalHandler);

    // ── Step 1: Register all compiled POUs ──
    POURegistry reg;
    registerAllPOUs(reg);
    RT_LOG_INFO("Registered %d POU(s):\n", reg.count());
    for (int i = 0; i < reg.count(); i++)
        RT_LOG_INFO("  [%d] %s (init=%s cyclic=%s)\n", i, reg.entries()[i].name,
                    reg.entries()[i].cbs.init ? "Y" : "N",
                    reg.entries()[i].cbs.cyclic ? "Y" : "N");
    RT_LOG_INFO("\n");

    // ── Step 2: Load configuration ──
    const char* cfgPath = (argc > 1 && argv[1][0] != '-') ? argv[1] : "tasks.json";
    json_cfg::Config cfg = json_cfg::Config::loadOrDefault(cfgPath);
    RT_LOG_INFO("Config: base_cycle=%dus  tasks=%zu\n\n",
                cfg.baseCycleUs, cfg.tasks.size());

    // ── Step 3: Create Scheduler ──
    Scheduler sched;
    CompositeTCI compositeTCI;
    sched.setBaseCycle(T_us(cfg.baseCycleUs));
    sched.watchdog.setDefault(T_ms(10));
    sched.gvl.clear();
    sched.gvl.errorMgr = &sched.errorMgr;

    // ─── 初始化 TCI ───
#if defined(RT_ETHERCAT_ENABLED)
    if (tciMode == TciMode::ETHERCAT) {
        static EthercatTCI ecatTCI;
        if (ecatTCI.init(ecatIfname) == 0) {
            compositeTCI.add(&ecatTCI);
            RT_LOG_INFO("EtherCAT TCI registered on %s\n", ecatIfname);
        } else {
            RT_LOG_INFO("EtherCAT TCI init failed on %s\n", ecatIfname);
        }
    }
#endif

    if (compositeTCI.count() > 0) {
        sched.setTCI(&compositeTCI);
        RT_LOG_INFO("TCI: %d backend(s) active\n", compositeTCI.count());
    }

    bool anyPouScheduled = false;

    for (const auto& tc : cfg.tasks) {
        int taskIdx = sched.addCyclicTask(tc.name.c_str(), tc.priority, T_us(tc.intervalUs));
        int pousAdded = 0;
        for (const auto& pouName : tc.pous) {
            const auto* cbs = reg.lookup(pouName.c_str());
            if (cbs && cbs->cyclic) {
                int progIdx = sched.addProgram(pouName.c_str(), cbs->init, cbs->cyclic, cbs->pre, cbs->post);
                if (progIdx >= 0) {
                    sched.addProgramToTask(taskIdx, progIdx);
                    pousAdded++;
                }
            } else {
                RT_LOG_INFO("  [WARN] POU \"%s\" not found in registry\n", pouName.c_str());
            }
        }
        if (pousAdded > 0) {
            sched.setTaskWatchdog(taskIdx, T_us(tc.watchdogUs));
            anyPouScheduled = true;
            RT_LOG_INFO("  Task: %s  priority=%d  interval=%dus  watchdog=%dus  pous=%d\n",
                        tc.name.c_str(), tc.priority, tc.intervalUs, tc.watchdogUs, pousAdded);
        }
    }

    // ── Fallback: no tasks configured → register each POU in its own task ──
    if (!anyPouScheduled && reg.count() > 0) {
        RT_LOG_INFO("  (no tasks configured — auto-registering each POU)\n");
        for (int i = 0; i < reg.count(); i++) {
            const auto& e = reg.entries()[i];
            int taskIdx = sched.addCyclicTask(e.name, 5, T_ms(10));
            int progIdx = sched.addProgram(e.name, e.cbs.init, e.cbs.cyclic, e.cbs.pre, e.cbs.post);
            if (progIdx >= 0) {
                sched.addProgramToTask(taskIdx, progIdx);
            }
            sched.setTaskWatchdog(taskIdx, T_ms(5));
            RT_LOG_INFO("  Task: %s  priority=5  interval=10ms  watchdog=5ms\n",
                        e.name);
        }
    }

    if (!jitterOnly) {
        sched.start(StartupMode::COLD);
        RT_LOG_INFO("Running... press Ctrl+C to stop\n");
    } else {
        RT_LOG_INFO("Jitter-only mode (no PLC scheduler)\n");
    }

    jitterReset(cycleUs);
    int64_t lastDiag = 0;
    uint64_t tickCount = 0;

    while (running) {
        int64_t now = platform::steadyUs();

        if (!jitterOnly) {
            sched.tick();
            tickCount++;
        }

        jitterSample(now);

        // Wait until next cycle
        int64_t next = now + cycleUs;
        int64_t sleepUs = next - platform::steadyUs();
        if (sleepUs > 0) platform::sleepUs(sleepUs);
        // if sleepUs <= 0, we're already late (jitter will reflect this)

        // Diagnostic output
        now = platform::steadyUs();
        if (now - lastDiag > (int64_t)diagIntervalS * 1000000) {
            lastDiag = now;
            if (jitter.count > 0) {
                int64_t avg = jitter.sumJitter / (int64_t)jitter.count;
                double variance = jitter.count > 1
                    ? (double)jitter.sumSquareJitter / jitter.count - (double)avg * avg
                    : 0;
                platform::logInfo("[DIAG] ticks=%llu  [JITTER] min=%lld max=%lld avg=%lld stddev=%lld (samples=%llu)\n",
                    (unsigned long long)tickCount,
                    (long long)jitter.minJitter, (long long)jitter.maxJitter,
                    (long long)avg,
                    (long long)(variance > 0 ? (int64_t)std::sqrt(variance) : 0),
                    (unsigned long long)jitter.count);
            }
        }
    }

    if (!jitterOnly) sched.stop();
    printJitterStats();
    platform::logInfo("Stopped. Total ticks: %llu\n", (unsigned long long)tickCount);
    return 0;
}

