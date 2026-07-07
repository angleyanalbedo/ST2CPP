#include "debug/repl_engine.h"
#include "core/types.h"
#include <cstring>
#include <cstdlib>

namespace rt_plc {

void ReplEngine::begin(const DebugMapEntry* map, uint32_t map_count) {
    map_ = map;
    map_count_ = map_count;
    watch_count_ = 0;
    force_count_ = 0;
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
    applyForces();
}

const DebugMapEntry* ReplEngine::findEntryById(uint32_t id) const {
    for (uint32_t i = 0; i < map_count_; i++) {
        if (map_[i].id == id) return &map_[i];
    }
    return nullptr;
}

const DebugMapEntry* ReplEngine::findEntryByName(const char* name) const {
    for (uint32_t i = 0; i < map_count_; i++) {
        if (strcmp(map_[i].name, name) == 0) return &map_[i];
    }
    return nullptr;
}

int ReplEngine::findChildren(uint32_t parentId, const DebugMapEntry** out, int max) const {
    int n = 0;
    for (uint32_t i = 0; i < map_count_ && n < max; i++) {
        if (map_[i].parentId == parentId) {
            out[n++] = &map_[i];
        }
    }
    return n;
}

const DebugMapEntry* ReplEngine::findEntry(const char* name_or_id) const {
    if (name_or_id[0] >= '0' && name_or_id[0] <= '9')
        if (const auto* e = findEntryById((uint32_t)atoi(name_or_id)))
            return e;
    return findEntryByName(name_or_id);
}

int ReplEngine::findEntries(const char* name, const DebugMapEntry** out, int max) const {
    for (uint32_t i = 0; i < map_count_; i++) {
        if (strcmp(map_[i].name, name) == 0) {
            out[0] = &map_[i];
            return 1;
        }
    }
    int n = 0;
    size_t len = strlen(name);
    for (uint32_t i = 0; i < map_count_ && n < max; i++) {
        size_t nlen = strlen(map_[i].name);
        if (nlen > len && map_[i].name[nlen - len - 1] == '.'
            && strcmp(map_[i].name + nlen - len, name) == 0) {
            out[n++] = &map_[i];
        }
    }
    return n;
}

const DebugMapEntry* ReplEngine::resolveEntry(const char* name, char* out, size_t out_size) const {
    const DebugMapEntry* ents[8];
    int n = findEntries(name, ents, 8);
    if (n == 0) {
        snprintf(out, out_size, "Error: unknown '%s'\n", name);
        return nullptr;
    }
    if (n > 1) {
        size_t pos = snprintf(out, out_size, "Ambiguous '%s' matches:\n", name);
        for (int i = 0; i < n && pos < out_size; i++) {
            int r = snprintf(out + pos, out_size - pos, "  %s\n", ents[i]->name);
            if (r > 0) pos += r;
        }
        return nullptr;
    }
    return ents[0];
}

bool ReplEngine::isForced(uint32_t id) const {
    for (int i = 0; i < force_count_; i++)
        if (force_ids_[i] == id) return true;
    return false;
}

int ReplEngine::doForce(const DebugMapEntry* ent, const char* val_str) {
    uint8_t buf[8] = {};
    size_t total = (size_t)ent->size * ent->count;
    if (total > sizeof(buf)) return -1;

    if (ent->bitOffset != 0xFF) {
        uint8_t byte_val = 0;
        readVar(ent, &byte_val, 1);
        uint8_t bit_val = (strcmp(val_str, "TRUE") == 0 || strcmp(val_str, "true") == 0 ||
                           strcmp(val_str, "1") == 0) ? 1 : 0;
        if (bit_val) byte_val |= (1 << ent->bitOffset);
        else byte_val &= ~(1 << ent->bitOffset);
        writeVar(ent, &byte_val, 1);
        buf[0] = byte_val;
    } else {
        if (!writeVarFromString(ent, val_str)) return -1;
        if (!readVar(ent, buf, total)) return -1;
    }

    for (int i = 0; i < force_count_; i++) {
        if (force_ids_[i] == ent->id) {
            memcpy(force_vals_[i], buf, total);
            return 0;
        }
    }
    if (force_count_ < MAX_FORCE) {
        force_ids_[force_count_] = ent->id;
        memcpy(force_vals_[force_count_], buf, total);
        force_count_++;
        return 0;
    }
    return -2;
}

int ReplEngine::doUnforce(const DebugMapEntry* ent) {
    for (int i = 0; i < force_count_; i++) {
        if (force_ids_[i] == ent->id) {
            force_ids_[i] = force_ids_[force_count_ - 1];
            memcpy(force_vals_[i], force_vals_[force_count_ - 1], 8);
            force_count_--;
            return 0;
        }
    }
    return -1;
}

void ReplEngine::applyForces() {
    for (int i = 0; i < force_count_; i++) {
        const DebugMapEntry* ent = findEntryById(force_ids_[i]);
        if (ent) {
            writeVar(ent, force_vals_[i], (size_t)ent->size * ent->count);
        }
    }
}

bool ReplEngine::readVar(const DebugMapEntry* ent, uint8_t* buf, size_t buf_size) const {
    size_t total = (size_t)ent->size * ent->count;
    if (buf_size < total) return false;

    switch (ent->storage) {
    case DebugStorage::GVL:
        if (ent->offset + total > GVL_SIZE) return false;
        memcpy(buf, gvl_.memory + ent->offset, total);
        return true;
    case DebugStorage::INPUT:
        if (ent->offset + total > PROCESS_IMAGE_SIZE) return false;
        memcpy(buf, image_.inputs + ent->offset, total);
        return true;
    case DebugStorage::OUTPUT:
        if (ent->offset + total > PROCESS_IMAGE_SIZE) return false;
        memcpy(buf, image_.outputs + ent->offset, total);
        return true;
    default:
        return false;
    }
}

bool ReplEngine::writeVar(const DebugMapEntry* ent, const uint8_t* data, size_t data_size) {
    size_t total = (size_t)ent->size * ent->count;
    if (data_size < total) return false;

    switch (ent->storage) {
    case DebugStorage::GVL:
        if (ent->offset + total > GVL_SIZE) return false;
        memcpy(gvl_.memory + ent->offset, data, total);
        return true;
    case DebugStorage::INPUT:
        if (ent->offset + total > PROCESS_IMAGE_SIZE) return false;
        memcpy(image_.inputs + ent->offset, data, total);
        return true;
    case DebugStorage::OUTPUT:
        if (ent->offset + total > PROCESS_IMAGE_SIZE) return false;
        memcpy(image_.outputs + ent->offset, data, total);
        return true;
    default:
        return false;
    }
}

void ReplEngine::formatValue(DebugType type, const uint8_t* buf,
                              char* out, size_t out_size) const {
    switch (type) {
    case DebugType::BOOL: {
        BOOL v; memcpy(&v, buf, sizeof(BOOL));
        snprintf(out, out_size, "%s", v ? "TRUE" : "FALSE");
        break;
    }
    case DebugType::SINT: {
        SINT v; memcpy(&v, buf, sizeof(SINT));
        snprintf(out, out_size, "%d", (int)v);
        break;
    }
    case DebugType::INT: {
        INT v; memcpy(&v, buf, sizeof(INT));
        snprintf(out, out_size, "%d", (int)v);
        break;
    }
    case DebugType::DINT: {
        DINT v; memcpy(&v, buf, sizeof(DINT));
        snprintf(out, out_size, "%d", v);
        break;
    }
    case DebugType::LINT: {
        LINT v; memcpy(&v, buf, sizeof(LINT));
        snprintf(out, out_size, "%lld", (long long)v);
        break;
    }
    case DebugType::USINT: {
        USINT v; memcpy(&v, buf, sizeof(USINT));
        snprintf(out, out_size, "%u", (unsigned)v);
        break;
    }
    case DebugType::UINT: {
        UINT v; memcpy(&v, buf, sizeof(UINT));
        snprintf(out, out_size, "%u", (unsigned)v);
        break;
    }
    case DebugType::UDINT: {
        UDINT v; memcpy(&v, buf, sizeof(UDINT));
        snprintf(out, out_size, "%u", v);
        break;
    }
    case DebugType::ULINT: {
        ULINT v; memcpy(&v, buf, sizeof(ULINT));
        snprintf(out, out_size, "%llu", (unsigned long long)v);
        break;
    }
    case DebugType::REAL: {
        REAL v; memcpy(&v, buf, sizeof(REAL));
        char tmp[32]; snprintf(tmp, sizeof(tmp), "%g", v);
        snprintf(out, out_size, "%s", tmp);
        break;
    }
    case DebugType::LREAL: {
        LREAL v; memcpy(&v, buf, sizeof(LREAL));
        char tmp[32]; snprintf(tmp, sizeof(tmp), "%g", v);
        snprintf(out, out_size, "%s", tmp);
        break;
    }
    case DebugType::BYTE: {
        BYTE v; memcpy(&v, buf, sizeof(BYTE));
        snprintf(out, out_size, "16#%02X", v);
        break;
    }
    case DebugType::WORD: {
        WORD v; memcpy(&v, buf, sizeof(WORD));
        snprintf(out, out_size, "16#%04X", v);
        break;
    }
    case DebugType::DWORD: {
        DWORD v; memcpy(&v, buf, sizeof(DWORD));
        snprintf(out, out_size, "16#%08X", v);
        break;
    }
    case DebugType::TIME: {
        TIME v; memcpy(&v, buf, sizeof(TIME));
        if (v >= 1000000)
            snprintf(out, out_size, "%llds %lldms", (long long)(v/1000000), (long long)((v%1000000)/1000));
        else if (v >= 1000)
            snprintf(out, out_size, "%lldms", (long long)(v/1000));
        else
            snprintf(out, out_size, "%lldus", (long long)v);
        break;
    }
    default:
        snprintf(out, out_size, "(type=%d)", (int)type);
        break;
    }
}

bool ReplEngine::writeVarFromString(const DebugMapEntry* ent, const char* str) {
    uint8_t buf[8] = {};
    size_t write_size = ent->size;

    switch (ent->type) {
    case DebugType::BOOL: {
        BOOL v = FALSE;
        if (strcmp(str, "TRUE") == 0 || strcmp(str, "true") == 0 || strcmp(str, "1") == 0) v = TRUE;
        memcpy(buf, &v, sizeof(v));
        break;
    }
    case DebugType::SINT: { SINT v = (SINT)atoi(str); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::INT:  { INT v = (INT)atoi(str); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::DINT: { DINT v = (DINT)atol(str); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::LINT: { LINT v = (LINT)atoll(str); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::USINT:{ USINT v = (USINT)atoi(str); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::UINT: { UINT v = (UINT)atoi(str); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::UDINT:{ UDINT v = (UDINT)atol(str); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::ULINT:{ ULINT v = (ULINT)atoll(str); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::REAL: { REAL v = (REAL)atof(str); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::LREAL:{ LREAL v = atof(str); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::BYTE: { BYTE v = (BYTE)strtol(str, nullptr, 0); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::WORD: { WORD v = (WORD)strtol(str, nullptr, 0); memcpy(buf, &v, sizeof(v)); break; }
    case DebugType::DWORD:{ DWORD v = (DWORD)strtoul(str, nullptr, 0); memcpy(buf, &v, sizeof(v)); break; }
    default: return false;
    }

    return writeVar(ent, buf, write_size);
}

void ReplEngine::printEntry(char* out, size_t out_size, const DebugMapEntry* ent) {
    if (ent->bitOffset != 0xFF) {
        uint8_t byte_val = 0;
        if (readVar(ent, &byte_val, 1)) {
            uint8_t bit = (byte_val >> ent->bitOffset) & 1;
            const char* forced = isForced(ent->id) ? " [FORCED]" : "";
            snprintf(out, out_size, "  %s = %s%s\n", ent->name, bit ? "TRUE" : "FALSE", forced);
        } else {
            snprintf(out, out_size, "  %s = (read error)\n", ent->name);
        }
        return;
    }

    uint8_t buf[256];
    size_t total = (size_t)ent->size * ent->count;
    if (total > sizeof(buf)) {
        snprintf(out, out_size, "  %s = (too large: %u bytes)\n", ent->name, total);
        return;
    }

    if (!readVar(ent, buf, total)) {
        snprintf(out, out_size, "  %s = (read error)\n", ent->name);
        return;
    }

    char val_str[64];
    if (ent->parentId == 0 && ent->count == 1) {
        // 检查是否为 struct（有子节点）
        const DebugMapEntry* children[64];
        int n = findChildren(ent->id, children, 64);
        if (n > 0) {
            snprintf(out, out_size, "  %s = (struct, %u bytes)\n", ent->name, total);
            return;
        }
    }

    formatValue(ent->type, buf, val_str, sizeof(val_str));

    const char* forced = isForced(ent->id) ? " [FORCED]" : "";

    if (ent->count > 1) {
        snprintf(out, out_size, "  %s[%u] = %s%s\n", ent->name, ent->count, val_str, forced);
    } else {
        snprintf(out, out_size, "  %s = %s%s\n", ent->name, val_str, forced);
    }
}

void ReplEngine::printChildren(char* out, size_t out_size, uint32_t parentId, int depth) {
    const DebugMapEntry* children[64];
    int n = findChildren(parentId, children, 64);
    size_t pos = 0;
    for (int i = 0; i < n && pos < out_size; i++) {
        char line[128];
        printEntry(line, sizeof(line), children[i]);
        size_t len = strlen(line);
        if (pos + len < out_size) {
            memcpy(out + pos, line, len);
            pos += len;
        }
    }
    out[pos] = '\0';
}

void ReplEngine::printAll(char* out, size_t out_size) {
    size_t pos = 0;
    for (uint32_t i = 0; i < map_count_ && pos < out_size; i++) {
        if (map_[i].parentId != 0) continue; // 只打印顶层
        char line[128];
        printEntry(line, sizeof(line), &map_[i]);
        size_t len = strlen(line);
        if (pos + len < out_size) {
            memcpy(out + pos, line, len);
            pos += len;
        }
        // 打印子字段
        char children_buf[1024];
        printChildren(children_buf, sizeof(children_buf), map_[i].id, 1);
        size_t clen = strlen(children_buf);
        if (clen > 0 && pos + clen < out_size) {
            memcpy(out + pos, children_buf, clen);
            pos += clen;
        }
    }
    out[pos] = '\0';
}

void ReplEngine::printWatched(char* out, size_t out_size) {
    size_t pos = 0;
    for (int i = 0; i < watch_count_ && pos < out_size; i++) {
        const DebugMapEntry* ent = findEntryById(watch_ids_[i]);
        if (!ent) continue;
        char line[128];
        printEntry(line, sizeof(line), ent);
        size_t len = strlen(line);
        if (pos + len < out_size) {
            memcpy(out + pos, line, len);
            pos += len;
        }
    }
    out[pos] = '\0';
}

void ReplEngine::printForces(char* out, size_t out_size) {
    if (force_count_ == 0) {
        snprintf(out, out_size, "  (no forced variables)\n");
        return;
    }
    size_t pos = 0;
    for (int i = 0; i < force_count_ && pos < out_size; i++) {
        const DebugMapEntry* ent = findEntryById(force_ids_[i]);
        if (!ent) continue;
        char line[128];
        printEntry(line, sizeof(line), ent);
        size_t len = strlen(line);
        if (pos + len < out_size) {
            memcpy(out + pos, line, len);
            pos += len;
        }
    }
    out[pos] = '\0';
}

void ReplEngine::printPrograms(char* out, size_t out_size) {
    size_t pos = 0;
    int n = reg_.count();
    for (int i = 0; i < n && pos < out_size; i++) {
        const auto& e = reg_.entries()[i];
        int r = snprintf(out + pos, out_size - pos, "  %s\n", e.name);
        if (r > 0) pos += r;
    }
    if (pos == 0) {
        snprintf(out, out_size, "  (no programs registered)\n");
    }
}

int ReplEngine::execCommand(const char* line, char* out, size_t out_size) {
    if (out == nullptr || out_size == 0) return -1;
    out[0] = '\0';
    if (line == nullptr || *line == '\0') return 0;

    while (*line == ' ' || *line == '\t') line++;
    if (*line == '\0' || *line == '\n' || *line == '\r') return 0;

    char cmd[64], arg1[256], arg2[256];
    int parsed = sscanf(line, "%63s %255s %255s", cmd, arg1, arg2);
    if (parsed < 1) return 0;

    if (strcmp(cmd, "help") == 0 || strcmp(cmd, "h") == 0) {
        snprintf(out, out_size,
            "Commands:\n"
            "  help / h              Show this help\n"
            "  list / ls             List all variables\n"
            "  programs              List registered POU programs\n"
            "  get <name/id>         Read a variable\n"
            "  set <name/id> <val>   Write a variable (one cycle)\n"
            "  run [N]               Execute N scan cycles\n"
            "  step                  Execute 1 scan cycle\n"
            "  watch <name>          Add variable to watch list\n"
            "  watch list            Show watch list\n"
            "  watch clear           Clear watch list\n"
            "  force <name> <val>    Force a variable (persists across cycles)\n"
            "  unforce <name>        Remove force from a variable\n"
            "  forces                List all forced variables\n"
            "  quit / exit / q       Exit\n");
        return 0;
    }

    if (strcmp(cmd, "list") == 0 || strcmp(cmd, "ls") == 0) {
        printAll(out, out_size);
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
            printWatched(out + pos, out_size - pos);
        }
        return 0;
    }

    if (strcmp(cmd, "step") == 0) {
        return execCommand("run 1", out, out_size);
    }

    if (strcmp(cmd, "programs") == 0 || strcmp(cmd, "prog") == 0) {
        printPrograms(out, out_size);
        return 0;
    }

    if (strcmp(cmd, "get") == 0) {
        if (parsed < 2) { snprintf(out, out_size, "Usage: get <name/id>\n"); return -1; }

        const DebugMapEntry* ent = findEntry(arg1);
        if (!ent) { ent = resolveEntry(arg1, out, out_size); if (!ent) return -1; }

        printEntry(out, out_size, ent);

        // 附加子字段
        size_t pos = strlen(out);
        if (ent->parentId == 0) {
            char children[2048];
            printChildren(children, sizeof(children), ent->id, 1);
            size_t clen = strlen(children);
            if (clen > 0 && pos + clen < out_size) {
                memcpy(out + pos, children, clen);
                pos += clen;
            }
        }
        return 0;
    }

    if (strcmp(cmd, "set") == 0) {
        if (parsed < 3) { snprintf(out, out_size, "Usage: set <name/id> <value>\n"); return -1; }

        const DebugMapEntry* ent = findEntry(arg1);
        if (!ent) { ent = resolveEntry(arg1, out, out_size); if (!ent) return -1; }

        if (ent->bitOffset != 0xFF) {
            uint8_t byte_val = 0;
            readVar(ent, &byte_val, 1);
            uint8_t bit_val = (strcmp(arg2, "TRUE") == 0 || strcmp(arg2, "true") == 0 ||
                               strcmp(arg2, "1") == 0) ? 1 : 0;
            if (bit_val) byte_val |= (1 << ent->bitOffset);
            else byte_val &= ~(1 << ent->bitOffset);
            writeVar(ent, &byte_val, 1);
            snprintf(out, out_size, "OK\n");
            return 0;
        }

        if (writeVarFromString(ent, arg2)) {
            snprintf(out, out_size, "OK\n");
        } else {
            snprintf(out, out_size, "Error: failed to write\n");
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
                    const DebugMapEntry* w = findEntryById(watch_ids_[i]);
                    int r = snprintf(out + pos, out_size - pos, "  %s\n", w ? w->name : "?");
                    if (r > 0) pos += r;
                }
            }
            return 0;
        }
        const DebugMapEntry* ent = findEntry(arg1);
        if (!ent) { ent = resolveEntry(arg1, out, out_size); if (!ent) return -1; }
        for (int i = 0; i < watch_count_; i++) {
            if (watch_ids_[i] == ent->id) {
                snprintf(out, out_size, "Already watching\n");
                return 0;
            }
        }
        if (watch_count_ < MAX_WATCH) {
            watch_ids_[watch_count_++] = ent->id;
            snprintf(out, out_size, "Watching %s (id=%u)\n", ent->name, ent->id);
        } else {
            snprintf(out, out_size, "Watch list full (%d max)\n", MAX_WATCH);
        }
        return 0;
    }

    if (strcmp(cmd, "force") == 0) {
        if (parsed < 3) { snprintf(out, out_size, "Usage: force <name> <value>\n"); return -1; }
        const DebugMapEntry* ent = findEntry(arg1);
        if (!ent) { ent = resolveEntry(arg1, out, out_size); if (!ent) return -1; }
        int ret = doForce(ent, arg2);
        if (ret == 0) {
            snprintf(out, out_size, "Forced ");
            printEntry(out + strlen(out), out_size - strlen(out), ent);
        } else if (ret == -2) {
            snprintf(out, out_size, "Error: force table full\n");
        } else {
            snprintf(out, out_size, "Error: failed to force\n");
        }
        return 0;
    }

    if (strcmp(cmd, "unforce") == 0) {
        if (parsed < 2) { snprintf(out, out_size, "Usage: unforce <name>\n"); return -1; }
        const DebugMapEntry* ent = findEntry(arg1);
        if (!ent) { ent = resolveEntry(arg1, out, out_size); if (!ent) return -1; }
        if (doUnforce(ent) == 0) {
            snprintf(out, out_size, "Unforced %s\n", ent->name);
        } else {
            snprintf(out, out_size, "%s is not forced\n", ent->name);
        }
        return 0;
    }

    if (strcmp(cmd, "forces") == 0) {
        printForces(out, out_size);
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
    char result[8192];
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
