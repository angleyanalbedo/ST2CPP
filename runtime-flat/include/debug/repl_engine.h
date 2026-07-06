#pragma once

#include "debug_if.h"
#include "core/gvl.h"
#include "core/registry.h"
#include "rt_plc.h"
#include <cstdint>
#include <cstdio>

namespace rt_plc {

struct NameEntry {
    uint32_t id;
    const char* name;
};

class ReplEngine {
public:
    ReplEngine() = default;

    void begin(const DebugVar* vars, uint32_t var_count,
               const NameEntry* names, uint32_t name_count);

    POURegistry& registry() { return reg_; }

    void initAll();
    void tickAll(TIME cycleTimeUs = 20000);

    void run();

    int execCommand(const char* line, char* out, size_t out_size);

private:
    GVL gvl_;
    ProcessImage image_;
    POURegistry reg_;
    const DebugVar* vars_ = nullptr;
    uint32_t var_count_ = 0;
    const NameEntry* names_ = nullptr;
    uint32_t name_count_ = 0;
    uint64_t cycle_count_ = 0;
    bool done_ = false;

    static constexpr int MAX_WATCH = 32;
    uint32_t watch_ids_[MAX_WATCH];
    int watch_count_ = 0;

    const DebugVar* findVarById(uint32_t id) const;
    const NameEntry* findNameById(uint32_t id) const;
    uint32_t findIdByName(const char* name) const;

    bool readVar(uint32_t id, uint8_t* buf, size_t buf_size) const;
    bool writeVar(uint32_t id, const uint8_t* data, size_t data_size);
    bool writeVarFromString(uint32_t id, const char* str);

    void formatValue(DebugType type, const uint8_t* buf, uint16_t size, char* out, size_t out_size) const;
    void printVar(char* out, size_t out_size, uint32_t id);
    void printAllVars(char* out, size_t out_size);
    void printWatched(char* out, size_t out_size);
};

} // namespace rt_plc
