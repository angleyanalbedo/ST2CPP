#pragma once

#include "debug_if.h"
#include "core/gvl.h"
#include "core/registry.h"
#include "rt_plc.h"
#include <cstdint>
#include <cstdio>

namespace rt_plc {

// REPL 专用变量表条目 — 含名字 + parentId，不进入固件
struct DebugMapEntry {
    uint32_t     id;
    const char*  name;
    DebugStorage storage;
    uint8_t      bitOffset;
    DebugType    type;
    DebugAccess  access;
    uint32_t     offset;
    uint16_t     size;
    uint16_t     count;
    uint32_t     parentId;   // 0 = top-level, >0 = struct 字段
};

class ReplEngine {
public:
    ReplEngine() = default;

    void begin(const DebugMapEntry* map, uint32_t map_count);

    POURegistry& registry() { return reg_; }

    void initAll();
    void tickAll(TIME cycleTimeUs = 20000);

    void run();

    bool isDone() const { return done_; }

    int execCommand(const char* line, char* out, size_t out_size);

private:
    GVL gvl_;
    ProcessImage image_;
    POURegistry reg_;
    const DebugMapEntry* map_ = nullptr;
    uint32_t map_count_ = 0;
    uint64_t cycle_count_ = 0;
    bool done_ = false;

    static constexpr int MAX_WATCH = 32;
    uint32_t watch_ids_[MAX_WATCH];
    int watch_count_ = 0;

    static constexpr int MAX_FORCE = 32;
    uint32_t force_ids_[MAX_FORCE];
    uint8_t  force_vals_[MAX_FORCE][8];
    int      force_count_ = 0;

    const DebugMapEntry* findEntryById(uint32_t id) const;
    const DebugMapEntry* findEntryByName(const char* name) const;
    const DebugMapEntry* findEntry(const char* name_or_id) const;
    int findEntries(const char* name, const DebugMapEntry** out, int max) const;
    const DebugMapEntry* resolveEntry(const char* name, char* out, size_t out_size) const;
    int findChildren(uint32_t parentId, const DebugMapEntry** out, int max) const;

    bool isForced(uint32_t id) const;

    bool readVar(const DebugMapEntry* ent, uint8_t* buf, size_t buf_size) const;
    bool writeVar(const DebugMapEntry* ent, const uint8_t* data, size_t data_size);
    bool writeVarFromString(const DebugMapEntry* ent, const char* str);
    int  doForce(const DebugMapEntry* ent, const char* val_str);
    int  doUnforce(const DebugMapEntry* ent);
    void applyForces();

    void formatValue(DebugType type, const uint8_t* buf, char* out, size_t out_size) const;
    void printEntry(char* out, size_t out_size, const DebugMapEntry* ent);
    void printAll(char* out, size_t out_size);
    void printWatched(char* out, size_t out_size);
    void printChildren(char* out, size_t out_size, uint32_t parentId, int depth);
    void printForces(char* out, size_t out_size);
    void printPrograms(char* out, size_t out_size);
};

} // namespace rt_plc
