#include "debug/repl_engine.h"
#include "isocline.h"
#include <cstdio>

namespace rt_plc {
extern const DebugMapEntry st2c_debug_map[];
extern const uint32_t st2c_debug_map_count;
}

static void completer(ic_completion_env_t* cenv, const char* prefix) {
    static const char* cmds[] = {
        "help", "list", "get", "set", "run", "step", "watch", "quit", "exit", nullptr
    };
    ic_add_completions(cenv, prefix, cmds);
    uint32_t n = rt_plc::st2c_debug_map_count;
    for (uint32_t i = 0; i < n; i++) {
        if (ic_istarts_with(rt_plc::st2c_debug_map[i].name, prefix)) {
            if (!ic_add_completion(cenv, rt_plc::st2c_debug_map[i].name)) return;
        }
    }
}

int main() {
    rt_plc::ReplEngine repl;
    repl.begin(rt_plc::st2c_debug_map, rt_plc::st2c_debug_map_count);
    extern void registerPOU_combined(rt_plc::POURegistry& reg);
    registerPOU_combined(repl.registry());

    ic_set_default_completer(completer, nullptr);
    ic_set_history(".plc_repl_history", 200);
    ic_enable_auto_tab(true);

    char result[8192];
    while (true) {
        const char* line = ic_readline("plc_repl> ");
        if (line == nullptr) break;
        if (line[0] == '\0') { ic_free((void*)line); continue; }

        ic_history_add(line);
        repl.execCommand(line, result, sizeof(result));
        if (result[0]) printf("%s", result);

        ic_free((void*)line);
        if (repl.isDone()) break;
    }
    return 0;
}
