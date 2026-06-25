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
#include <cstdio>
#include <string>
#include <vector>
#include <fstream>
#include <sstream>
#include <thread>
#include <chrono>

using namespace rt_plc;

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
            std::printf("  [runtime] tasks.json not found, using defaults\n");
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
extern void registerAllPOUs(POURegistry& reg);

// ═══════════════════════════════════════════════════════
//  Main
// ═══════════════════════════════════════════════════════

int main(int argc, char* argv[]) {
    std::printf("=== ST2C++ Flat Runtime ===\n\n");

    // ── Step 1: Register all compiled POUs ──
    POURegistry reg;
    registerAllPOUs(reg);
    std::printf("Registered %d POU(s):\n", reg.count());
    for (int i = 0; i < reg.count(); i++)
        std::printf("  [%d] %s (init=%s cyclic=%s)\n", i, reg.entries()[i].name,
                    reg.entries()[i].cbs.init ? "Y" : "N",
                    reg.entries()[i].cbs.cyclic ? "Y" : "N");
    std::printf("\n");

    // ── Step 2: Load configuration ──
    const char* cfgPath = (argc > 1) ? argv[1] : "tasks.json";
    json_cfg::Config cfg = json_cfg::Config::loadOrDefault(cfgPath);
    std::printf("Config: base_cycle=%dus  tasks=%zu\n\n",
                cfg.baseCycleUs, cfg.tasks.size());

    // ── Step 3: Create Scheduler ──
    Scheduler sched;
    sched.setBaseCycle(T_us(cfg.baseCycleUs));
    sched.watchdog.setDefault(T_ms(10));
    sched.gvl.clear();
    sched.gvl.errorMgr = &sched.errorMgr;

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
                std::printf("  [WARN] POU \"%s\" not found in registry\n", pouName.c_str());
            }
        }
        if (pousAdded > 0) {
            sched.setTaskWatchdog(taskIdx, T_us(tc.watchdogUs));
            anyPouScheduled = true;
            std::printf("  Task: %s  priority=%d  interval=%dus  watchdog=%dus  pous=%d\n",
                        tc.name.c_str(), tc.priority, tc.intervalUs, tc.watchdogUs, pousAdded);
        }
    }

    // ── Fallback: no tasks configured → register each POU in its own task ──
    if (!anyPouScheduled && reg.count() > 0) {
        std::printf("  (no tasks configured — auto-registering each POU)\n");
        for (int i = 0; i < reg.count(); i++) {
            const auto& e = reg.entries()[i];
            int taskIdx = sched.addCyclicTask(e.name, 5, T_ms(10));
            int progIdx = sched.addProgram(e.name, e.cbs.init, e.cbs.cyclic, e.cbs.pre, e.cbs.post);
            if (progIdx >= 0) {
                sched.addProgramToTask(taskIdx, progIdx);
            }
            sched.setTaskWatchdog(taskIdx, T_ms(5));
            std::printf("  Task: %s  priority=5  interval=10ms  watchdog=5ms\n",
                        e.name);
        }
    }

    std::printf("\n--- Running 100 ticks ---\n\n");

    for (int i = 0; i < 100; i++) {
        sched.tick();
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }

    std::printf("\n");
    sched.printDiag();
    std::printf("\n=== Runtime Complete ===\n");
    return 0;
}

