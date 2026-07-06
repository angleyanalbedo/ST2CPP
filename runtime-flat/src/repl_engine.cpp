#include "debug/repl_engine.h"
#include "core/types.h"
#include <cstring>
#include <cstdlib>

namespace rt_plc {

void ReplEngine::begin(const DebugVar* vars, uint32_t var_count,
                       const NameEntry* names, uint32_t name_count) {
    vars_ = vars;
    var_count_ = var_count;
    names_ = names;
    name_count_ = name_count;
    watch_count_ = 0;
    cycle_count_ = 0;
    done_ = false;
}

void ReplEngine::initAll() {
    for (int i = 0; i < reg_.count(); i++) {
        const auto& entry = reg_.entries()[i];
        if (entry.cbs.init) {
            entry.cbs.init(gvl_, image_);
        }
    }
}

void ReplEngine::tickAll(TIME cycleTimeUs) {
    for (int i = 0; i < reg_.count(); i++) {
        const auto& entry = reg_.entries()[i];
        if (entry.cbs.cyclic) {
            entry.cbs.cyclic(gvl_, image_, cycleTimeUs);
        }
    }
}

const DebugVar* ReplEngine::findVarById(uint32_t id) const {
    for (uint32_t i = 0; i < var_count_; i++) {
        if (vars_[i].id == id) return &vars_[i];
    }
    return nullptr;
}

const NameEntry* ReplEngine::findNameById(uint32_t id) const {
    for (uint32_t i = 0; i < name_count_; i++) {
        if (names_[i].id == id) return &names_[i];
    }
    return nullptr;
}

uint32_t ReplEngine::findIdByName(const char* name) const {
    for (uint32_t i = 0; i < name_count_; i++) {
        if (strcmp(names_[i].name, name) == 0) return names_[i].id;
    }

    if (name[0] >= '0' && name[0] <= '9') {
        uint32_t id = (uint32_t)atoi(name);
        if (findVarById(id)) return id;
    }

    return 0;
}

bool ReplEngine::readVar(uint32_t id, uint8_t* buf, size_t buf_size) const {
    const DebugVar* var = findVarById(id);
    if (!var) return false;

    size_t total = (size_t)var->size * var->count;
    if (buf_size < total) return false;

    switch (var->storage) {
    case DebugStorage::GVL:
        if (var->offset + total > GVL_SIZE) return false;
        memcpy(buf, gvl_.memory + var->offset, total);
        return true;
    case DebugStorage::INPUT:
        if (var->offset + total > PROCESS_IMAGE_SIZE) return false;
        memcpy(buf, image_.inputs + var->offset, total);
        return true;
    case DebugStorage::OUTPUT:
        if (var->offset + total > PROCESS_IMAGE_SIZE) return false;
        memcpy(buf, image_.outputs + var->offset, total);
        return true;
    default:
        return false;
    }
}

bool ReplEngine::writeVar(uint32_t id, const uint8_t* data, size_t data_size) {
    const DebugVar* var = findVarById(id);
    if (!var) return false;

    size_t total = (size_t)var->size * var->count;
    if (data_size < total) return false;

    switch (var->storage) {
    case DebugStorage::GVL:
        if (var->offset + total > GVL_SIZE) return false;
        memcpy(gvl_.memory + var->offset, data, total);
        return true;
    case DebugStorage::INPUT:
        if (var->offset + total > PROCESS_IMAGE_SIZE) return false;
        memcpy(image_.inputs + var->offset, data, total);
        return true;
    case DebugStorage::OUTPUT:
        if (var->offset + total > PROCESS_IMAGE_SIZE) return false;
        memcpy(image_.outputs + var->offset, data, total);
        return true;
    default:
        return false;
    }
}

void ReplEngine::formatValue(DebugType type, const uint8_t* buf, uint16_t size,
                              char* out, size_t out_size) const {
    switch (type) {
    case DebugType::BOOL: {
        BOOL v;
        memcpy(&v, buf, (size < sizeof(BOOL)) ? size : sizeof(BOOL));
        snprintf(out, out_size, "%s", v ? "TRUE" : "FALSE");
        break;
    }
    case DebugType::SINT: {
        SINT v;
        memcpy(&v, buf, (size < sizeof(SINT)) ? size : sizeof(SINT));
        snprintf(out, out_size, "%d", v);
        break;
    }
    case DebugType::INT: {
        INT v;
        memcpy(&v, buf, (size < sizeof(INT)) ? size : sizeof(INT));
        snprintf(out, out_size, "%d", v);
        break;
    }
    case DebugType::DINT: {
        DINT v;
        memcpy(&v, buf, (size < sizeof(DINT)) ? size : sizeof(DINT));
        snprintf(out, out_size, "%d", v);
        break;
    }
    case DebugType::LINT: {
        LINT v;
        memcpy(&v, buf, (size < sizeof(LINT)) ? size : sizeof(LINT));
        snprintf(out, out_size, "%lld", (long long)v);
        break;
    }
    case DebugType::USINT: {
        USINT v;
        memcpy(&v, buf, (size < sizeof(USINT)) ? size : sizeof(USINT));
        snprintf(out, out_size, "%u", v);
        break;
    }
    case DebugType::UINT: {
        UINT v;
        memcpy(&v, buf, (size < sizeof(UINT)) ? size : sizeof(UINT));
        snprintf(out, out_size, "%u", v);
        break;
    }
    case DebugType::UDINT: {
        UDINT v;
        memcpy(&v, buf, (size < sizeof(UDINT)) ? size : sizeof(UDINT));
        snprintf(out, out_size, "%u", v);
        break;
    }
    case DebugType::ULINT: {
        ULINT v;
        memcpy(&v, buf, (size < sizeof(ULINT)) ? size : sizeof(ULINT));
        snprintf(out, out_size, "%llu", (unsigned long long)v);
        break;
    }
    case DebugType::REAL: {
        REAL v;
        memcpy(&v, buf, (size < sizeof(REAL)) ? size : sizeof(REAL));
        char tmp[32];
        snprintf(tmp, sizeof(tmp), "%g", v);
        snprintf(out, out_size, "%s", tmp);
        break;
    }
    case DebugType::LREAL: {
        LREAL v;
        memcpy(&v, buf, (size < sizeof(LREAL)) ? size : sizeof(LREAL));
        char tmp[32];
        snprintf(tmp, sizeof(tmp), "%g", v);
        snprintf(out, out_size, "%s", tmp);
        break;
    }
    case DebugType::BYTE: {
        BYTE v;
        memcpy(&v, buf, (size < sizeof(BYTE)) ? size : sizeof(BYTE));
        snprintf(out, out_size, "16#%02X", v);
        break;
    }
    case DebugType::WORD: {
        WORD v;
        memcpy(&v, buf, (size < sizeof(WORD)) ? size : sizeof(WORD));
        snprintf(out, out_size, "16#%04X", v);
        break;
    }
    case DebugType::DWORD: {
        DWORD v;
        memcpy(&v, buf, (size < sizeof(DWORD)) ? size : sizeof(DWORD));
        snprintf(out, out_size, "16#%08X", v);
        break;
    }
    case DebugType::TIME: {
        TIME v;
        memcpy(&v, buf, (size < sizeof(TIME)) ? size : sizeof(TIME));
        if (v >= 1000000) {
            snprintf(out, out_size, "%llds %lldms",
                     (long long)(v / 1000000), (long long)((v % 1000000) / 1000));
        } else if (v >= 1000) {
            snprintf(out, out_size, "%lldms", (long long)(v / 1000));
        } else {
            snprintf(out, out_size, "%lldus", (long long)v);
        }
        break;
    }
    default:
        snprintf(out, out_size, "(type=%d raw)", (int)type);
        break;
    }
}

bool ReplEngine::writeVarFromString(uint32_t id, const char* str) {
    const DebugVar* var = findVarById(id);
    if (!var) return false;

    uint8_t buf[8] = {};
    size_t write_size = var->size;

    switch (var->type) {
    case DebugType::BOOL: {
        BOOL v = FALSE;
        if (strcmp(str, "TRUE") == 0 || strcmp(str, "true") == 0 ||
            strcmp(str, "1") == 0) v = TRUE;
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::SINT: {
        SINT v = (SINT)atoi(str);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::INT: {
        INT v = (INT)atoi(str);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::DINT: {
        DINT v = (DINT)atol(str);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::LINT: {
        LINT v = (LINT)atoll(str);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::USINT: {
        USINT v = (USINT)atoi(str);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::UINT: {
        UINT v = (UINT)atoi(str);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::UDINT: {
        UDINT v = (UDINT)atol(str);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::ULINT: {
        ULINT v = (ULINT)atoll(str);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::REAL: {
        REAL v = (REAL)atof(str);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::LREAL: {
        LREAL v = atof(str);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::BYTE: {
        BYTE v = (BYTE)strtol(str, nullptr, 0);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::WORD: {
        WORD v = (WORD)strtol(str, nullptr, 0);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::DWORD: {
        DWORD v = (DWORD)strtoul(str, nullptr, 0);
        memcpy(buf, &v, sizeof(v));
        break;
    }
    default:
        return false;
    }

    return writeVar(id, buf, write_size);
}

void ReplEngine::printVar(char* out, size_t out_size, uint32_t id) {
    const DebugVar* var = findVarById(id);
    if (!var) {
        snprintf(out, out_size, "(unknown id=%u)\n", id);
        return;
    }

    const NameEntry* ne = findNameById(id);
    const char* var_name = ne ? ne->name : "(unnamed)";

    if (var->bitOffset != 0xFF) {
        uint8_t byte_val = 0;
        if (readVar(id, &byte_val, 1)) {
            uint8_t bit = (byte_val >> var->bitOffset) & 1;
            snprintf(out, out_size, "%s = %s\n", var_name, bit ? "TRUE" : "FALSE");
        } else {
            snprintf(out, out_size, "%s = (read error)\n", var_name);
        }
        return;
    }

    uint8_t buf[256];
    size_t total = (size_t)var->size * var->count;
    if (total > sizeof(buf)) {
        snprintf(out, out_size, "%s = (too large: %u bytes)\n", var_name, total);
        return;
    }

    if (!readVar(id, buf, total)) {
        snprintf(out, out_size, "%s = (read error)\n", var_name);
        return;
    }

    char val_str[64];
    formatValue(var->type, buf, var->size, val_str, sizeof(val_str));

    if (var->count > 1) {
        snprintf(out, out_size, "%s[%u] = %s\n", var_name, var->count, val_str);
    } else {
        snprintf(out, out_size, "%s = %s\n", var_name, val_str);
    }
}

void ReplEngine::printAllVars(char* out, size_t out_size) {
    size_t pos = 0;
    for (uint32_t i = 0; i < var_count_ && pos < out_size; i++) {
        char line[128];
        printVar(line, sizeof(line), vars_[i].id);
        size_t len = strlen(line);
        if (pos + len < out_size) {
            memcpy(out + pos, line, len);
            pos += len;
        }
    }
    out[pos] = '\0';
}

void ReplEngine::printWatched(char* out, size_t out_size) {
    size_t pos = 0;
    for (int i = 0; i < watch_count_ && pos < out_size; i++) {
        char line[128];
        printVar(line, sizeof(line), watch_ids_[i]);
        size_t len = strlen(line);
        if (pos + len < out_size) {
            memcpy(out + pos, line, len);
            pos += len;
        }
    }
    out[pos] = '\0';
}

int ReplEngine::execCommand(const char* line, char* out, size_t out_size) {
    if (out == nullptr || out_size == 0) return -1;
    out[0] = '\0';

    if (line == nullptr || *line == '\0') return 0;

    while (*line == ' ' || *line == '\t') line++;
    if (*line == '\0' || *line == '\n' || *line == '\r') return 0;

    char cmd[64];
    char arg1[256];
    char arg2[256];
    int parsed = sscanf(line, "%63s %255s %255s", cmd, arg1, arg2);

    if (parsed < 1) return 0;

    if (strcmp(cmd, "help") == 0 || strcmp(cmd, "h") == 0) {
        snprintf(out, out_size,
            "Commands:\n"
            "  help                  Show this help\n"
            "  list                  List all variables\n"
            "  get <name/id>         Read a variable\n"
            "  set <name/id> <val>   Write a variable\n"
            "  run [N]               Execute N cycles\n"
            "  step                  Execute 1 cycle\n"
            "  watch <name>          Add variable to watch list\n"
            "  watch clear           Clear watch list\n"
            "  quit                  Exit\n");
        return 0;
    }

    if (strcmp(cmd, "list") == 0 || strcmp(cmd, "ls") == 0) {
        printAllVars(out, out_size);
        return 0;
    }

    if (strcmp(cmd, "lifecycle") == 0) {
        for (int i = 0; i < reg_.count(); i++) {
            const auto& e = reg_.entries()[i];
            char tmp[256];
            size_t pos = 0;
            pos += snprintf(tmp + pos, sizeof(tmp) - pos, "  %s: init=%p cyclic=%p\n",
                           e.name, (void*)e.cbs.init, (void*)e.cbs.cyclic);
            size_t len = strlen(tmp);
            if (len < out_size - 1) {
                memcpy(out, tmp, len);
                out[len] = '\0';
            }
        }
        return 0;
    }

    if (strcmp(cmd, "run") == 0) {
        int n = (parsed >= 2) ? atoi(arg1) : 1;
        if (n < 1) n = 1;
        for (int i = 0; i < n; i++) tickAll();
        cycle_count_ += n;

        size_t pos = 0;
        char tmp[128];
        int r = snprintf(tmp, sizeof(tmp), "Executed %d cycle(s). Total: %llu\n",
                        n, (unsigned long long)cycle_count_);
        if (r > 0 && (size_t)r < out_size) {
            memcpy(out + pos, tmp, r);
            pos += r;
        }

        if (watch_count_ > 0) {
            size_t wpos = pos;
            printWatched(out + wpos, out_size - wpos);
        }
        return 0;
    }

    if (strcmp(cmd, "step") == 0) {
        return execCommand("run 1", out, out_size);
    }

    if (strcmp(cmd, "get") == 0) {
        if (parsed < 2) {
            snprintf(out, out_size, "Usage: get <name/id>\n");
            return -1;
        }
        uint32_t id = findIdByName(arg1);
        if (id == 0) {
            snprintf(out, out_size, "Error: unknown variable '%s'\n", arg1);
            return -1;
        }
        printVar(out, out_size, id);
        return 0;
    }

    if (strcmp(cmd, "set") == 0) {
        if (parsed < 3) {
            snprintf(out, out_size, "Usage: set <name/id> <value>\n");
            return -1;
        }
        uint32_t id = findIdByName(arg1);
        if (id == 0) {
            snprintf(out, out_size, "Error: unknown variable '%s'\n", arg1);
            return -1;
        }

        const DebugVar* var = findVarById(id);
        if (!var) {
            snprintf(out, out_size, "Error: id %u not in table\n", id);
            return -1;
        }

        if (var->bitOffset != 0xFF) {
            uint8_t byte_val = 0;
            readVar(id, &byte_val, 1);
            uint8_t bit_val = (strcmp(arg2, "TRUE") == 0 || strcmp(arg2, "true") == 0 ||
                               strcmp(arg2, "1") == 0) ? 1 : 0;
            if (bit_val) byte_val |= (1 << var->bitOffset);
            else byte_val &= ~(1 << var->bitOffset);
            writeVar(id, &byte_val, 1);
            snprintf(out, out_size, "OK\n");
            return 0;
        }

        if (writeVarFromString(id, arg2)) {
            snprintf(out, out_size, "OK\n");
        } else {
            snprintf(out, out_size, "Error: failed to write variable\n");
        }
        return 0;
    }

    if (strcmp(cmd, "watch") == 0) {
        if (parsed >= 2 && strcmp(arg1, "clear") == 0) {
            watch_count_ = 0;
            snprintf(out, out_size, "Watch list cleared\n");
            return 0;
        }
        if (parsed < 2) {
            if (watch_count_ == 0) {
                snprintf(out, out_size, "Watch list is empty\n");
            } else {
                size_t pos = 0;
                for (int i = 0; i < watch_count_ && pos < out_size; i++) {
                    const NameEntry* ne = findNameById(watch_ids_[i]);
                    const char* wn = ne ? ne->name : "(unnamed)";
                    int r = snprintf(out + pos, out_size - pos, "  %s\n", wn);
                    if (r > 0) pos += r;
                }
            }
            return 0;
        }
        uint32_t id = findIdByName(arg1);
        if (id == 0) {
            snprintf(out, out_size, "Error: unknown variable '%s'\n", arg1);
            return -1;
        }
        for (int i = 0; i < watch_count_; i++) {
            if (watch_ids_[i] == id) {
                snprintf(out, out_size, "Already watching\n");
                return 0;
            }
        }
        if (watch_count_ < MAX_WATCH) {
            watch_ids_[watch_count_++] = id;
            const NameEntry* ne = findNameById(id);
            snprintf(out, out_size, "Watching %s (id=%u)\n", ne ? ne->name : "?", id);
        } else {
            snprintf(out, out_size, "Watch list full (%d max)\n", MAX_WATCH);
        }
        return 0;
    }

    if (strcmp(cmd, "quit") == 0 || strcmp(cmd, "exit") == 0 || strcmp(cmd, "q") == 0) {
        snprintf(out, out_size, "Bye\n");
        done_ = true;
        return 0;
    }

    snprintf(out, out_size, "Unknown command: '%s' (try 'help')\n", cmd);
    return -1;
}

void ReplEngine::run() {
    char line[512];
    char result[4096];
    done_ = false;

    while (!done_) {
        printf("plc_repl> ");
        fflush(stdout);

        if (fgets(line, sizeof(line), stdin) == nullptr) break;

        char* nl = strchr(line, '\n');
        if (nl) *nl = '\0';

        execCommand(line, result, sizeof(result));
        if (result[0]) printf("%s", result);
    }
}

} // namespace rt_plc
