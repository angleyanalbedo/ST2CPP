#include "debug/debug_if.h"
#include <cstring>
#include <cstdio>

namespace rt_plc {

// ─── DebugEngine ───

DebugEngine::DebugEngine() {
    std::memset(snapBuf_, 0, sizeof(snapBuf_));
    std::memset(forceTable_, 0, sizeof(forceTable_));
    std::memset(pendingQ_, 0, sizeof(pendingQ_));
    std::memset(watchIds_, 0, sizeof(watchIds_));
}

void DebugEngine::init(const DebugVar* vars, uint32_t count, const uint8_t* buildId) {
    vars_    = vars;
    varCount_ = count;
    buildId_  = buildId;
}

void DebugEngine::bindMemory(GVL* gvl, ProcessImage* image) {
    boundGVL_ = gvl;
    boundIO_  = image;
}

// ── Metadata ──

void DebugEngine::hello(uint8_t outBuildId[16], uint32_t& outVarCount) const {
    if (buildId_) std::memcpy(outBuildId, buildId_, 16);
    else          std::memset(outBuildId, 0, 16);
    outVarCount = varCount_;
}

const DebugVar* DebugEngine::findVar(uint32_t id) const {
    if (!vars_) return nullptr;
    for (uint32_t i = 0; i < varCount_; i++) {
        if (vars_[i].id == id) return &vars_[i];
    }
    return nullptr;
}

// ── Snapshot ──

bool DebugEngine::requestSetWatchList(const uint32_t* ids, uint32_t count) {
    PendingCommand cmd;
    cmd.type  = PendingCmdType::SET_WATCH;
    cmd.size  = static_cast<uint16_t>(count > DEBUG_MAX_WATCH_VARS ? DEBUG_MAX_WATCH_VARS : count);
    for (uint32_t i = 0; i < cmd.size; i++) cmd.value[i] = static_cast<uint8_t>(ids[i] & 0xFF);
    return pushPendingCommand(cmd);
}

void DebugEngine::doSetWatchList(const uint32_t* ids, uint32_t count) {
    if (count > DEBUG_MAX_WATCH_VARS) count = DEBUG_MAX_WATCH_VARS;
    for (uint32_t i = 0; i < count; i++) watchIds_[i] = ids[i];
    watchCount_ = count;
}

void DebugEngine::publishSnapshot(const GVL& gvl, const ProcessImage& io,
                                   uint64_t cycle, TIME systemTime,
                                   const DiagStats& diag, SystemState state) {
    int writeIdx = 1 - activeSnapIdx_.load(std::memory_order_acquire);
    DebugSnapshot& snap = snapBuf_[writeIdx];

    snap.cycle       = cycle;
    snap.systemTime  = systemTime;
    snap.sampleCount = 0;

    for (uint32_t i = 0; i < watchCount_; i++) {
        const DebugVar* v = findVar(watchIds_[i]);
        if (!v) continue;

        // 跳过数组和大结构体（Q4：只存标量，数组走 RANGE）
        if (v->count > 1) continue;
        if (v->size > 8)  continue;

        DebugSample& s = snap.samples[snap.sampleCount];
        s.id   = v->id;
        s.size = v->size;

        if (v->bitOffset != 0xFF) {
            // BOOL 变量：bit 读写
            bool bit = false;
            switch (v->storage) {
                case DebugStorage::GVL: {
                    uint8_t byte = gvl.memory[v->offset];
                    bit = (byte >> v->bitOffset) & 1;
                    break;
                }
                case DebugStorage::INPUT:
                    bit = io.readInputBit(v->offset, v->bitOffset);
                    break;
                case DebugStorage::OUTPUT:
                    bit = io.readOutputBit(v->offset, v->bitOffset);
                    break;
                default: break;
            }
            s.value[0] = bit ? 1 : 0;
        } else {
            // 标量：memcpy
            const uint8_t* src = nullptr;
            switch (v->storage) {
                case DebugStorage::GVL:
                    src = gvl.memory + v->offset; break;
                case DebugStorage::INPUT:
                    src = io.inputs + v->offset; break;
                case DebugStorage::OUTPUT:
                    src = io.outputs + v->offset; break;
                default: break;
            }
            if (src) std::memcpy(s.value, src, v->size);
            else     std::memset(s.value, 0, v->size);
        }
        snap.sampleCount++;
    }

    // 诊断统计
    snap.totalTicks   = diag.totalScanCount;
    snap.lastScanTime = diag.lastScanTime;
    snap.maxScanTime  = diag.maxScanTime;
    snap.systemState  = state;

    activeSnapIdx_.store(writeIdx, std::memory_order_release);
}

const DebugSnapshot& DebugEngine::latestSnapshot() const {
    return snapBuf_[activeSnapIdx_.load(std::memory_order_acquire)];
}

bool DebugEngine::readVar(uint32_t id, uint8_t* outValue, uint16_t& outSize) const {
    const DebugSnapshot& snap = latestSnapshot();
    for (uint32_t i = 0; i < snap.sampleCount; i++) {
        if (snap.samples[i].id == id) {
            std::memcpy(outValue, snap.samples[i].value, snap.samples[i].size);
            outSize = snap.samples[i].size;
            return true;
        }
    }
    return false;
}

bool DebugEngine::readMemory(DebugStorage storage, uint32_t offset,
                              uint32_t size, uint8_t* out) const {
    const uint8_t* src = nullptr;
    switch (storage) {
        case DebugStorage::GVL:
            if (boundGVL_ && offset + size <= GVL_SIZE)
                src = boundGVL_->memory + offset;
            break;
        case DebugStorage::INPUT:
            if (boundIO_ && offset + size <= PROCESS_IMAGE_SIZE)
                src = boundIO_->inputs + offset;
            break;
        case DebugStorage::OUTPUT:
            if (boundIO_ && offset + size <= PROCESS_IMAGE_SIZE)
                src = boundIO_->outputs + offset;
            break;
        default: break;
    }
    if (!src) return false;
    std::memcpy(out, src, size);
    return true;
}

// ── Force ──

bool DebugEngine::requestForce(uint32_t id, const uint8_t* value, uint16_t size) {
    if (size > 8) return false;
    PendingCommand cmd;
    cmd.type = PendingCmdType::FORCE;
    cmd.id   = id;
    cmd.size = size;
    std::memcpy(cmd.value, value, size);
    return pushPendingCommand(cmd);
}

bool DebugEngine::requestUnforce(uint32_t id) {
    PendingCommand cmd;
    cmd.type = PendingCmdType::UNFORCE;
    cmd.id   = id;
    return pushPendingCommand(cmd);
}

void DebugEngine::requestClearForces() {
    PendingCommand cmd;
    cmd.type = PendingCmdType::CLEAR_FORCES;
    pushPendingCommand(cmd);
}

bool DebugEngine::isForced(uint32_t id) const {
    for (int i = 0; i < forceCount_; i++) {
        if (forceTable_[i].id == id && forceTable_[i].active) return true;
    }
    return false;
}

void DebugEngine::doSetForce(uint32_t id, const uint8_t* value, uint16_t size) {
    const DebugVar* v = findVar(id);
    if (!v) return;
    if (v->access == DebugAccess::READ_ONLY) return;
    if (size > v->size) return;

    int slot = findForceSlot(id);
    if (slot < 0) {
        if (forceCount_ >= DEBUG_MAX_FORCES) return;
        slot = forceCount_++;
    }
    ForceEntry& f = forceTable_[slot];
    f.id     = id;
    f.offset = v->offset;
    f.size   = size;
    std::memcpy(f.value, value, size);
    f.active = true;
}

void DebugEngine::doUnforce(uint32_t id) {
    for (int i = 0; i < forceCount_; i++) {
        if (forceTable_[i].id == id) {
            forceTable_[i].active = false;
            return;
        }
    }
}

void DebugEngine::doClearForces() {
    for (int i = 0; i < forceCount_; i++) forceTable_[i].active = false;
    forceCount_ = 0;
}

int DebugEngine::findForceSlot(uint32_t id) const {
    for (int i = 0; i < forceCount_; i++) {
        if (forceTable_[i].id == id) return i;
    }
    return -1;
}

void DebugEngine::applyForces(GVL& gvl, ProcessImage& io,
                               ForcePhase phase, SystemState state) {
    for (int i = 0; i < forceCount_; i++) {
        if (!forceTable_[i].active) continue;
        const DebugVar* v = findVar(forceTable_[i].id);
        if (!v) continue;

        // Phase 过滤
        if (phase == ForcePhase::PRE_LOGIC) {
            if (v->storage != DebugStorage::INPUT) continue;
        } else { // POST_LOGIC
            if (v->storage != DebugStorage::OUTPUT && v->storage != DebugStorage::GVL)
                continue;
        }

        // 安全优先级：ERROR 时不应用 OUTPUT force
        if (state != SystemState::RUN && v->storage == DebugStorage::OUTPUT)
            continue;

        uint8_t* target = nullptr;
        switch (v->storage) {
            case DebugStorage::GVL:
                target = gvl.memory + v->offset; break;
            case DebugStorage::INPUT:
                target = io.inputs + v->offset; break;
            case DebugStorage::OUTPUT:
                target = io.outputs + v->offset; break;
            default: break;
        }
        if (!target) continue;

        if (v->bitOffset != 0xFF) {
            // BOOL bit force
            bool bitVal = (forceTable_[i].value[0] != 0);
            if (v->storage == DebugStorage::INPUT)
                io.writeInputBit(v->offset, v->bitOffset, bitVal);
            else if (v->storage == DebugStorage::OUTPUT)
                io.writeOutputBit(v->offset, v->bitOffset, bitVal);
            else {
                uint8_t mask = 1 << v->bitOffset;
                if (bitVal) *target |= mask;
                else        *target &= ~mask;
            }
        } else {
            std::memcpy(target, forceTable_[i].value, forceTable_[i].size);
        }
    }
}

// ── Pending Command Queue ──

bool DebugEngine::pushPendingCommand(const PendingCommand& cmd) {
    uint32_t next = (qHead_ + 1) % DEBUG_MAX_PENDING_COMMANDS;
    if (next == qTail_) {
        // Q3: 满了，丢最旧的
        qTail_ = (qTail_ + 1) % DEBUG_MAX_PENDING_COMMANDS;
        droppedCommands_++;
    }
    pendingQ_[qHead_] = cmd;
    qHead_ = next;
    return true;
}

void DebugEngine::applyPendingCommands() {
    while (qTail_ != qHead_) {
        const PendingCommand& cmd = pendingQ_[qTail_];
        switch (cmd.type) {
            case PendingCmdType::SET_WATCH: {
                uint32_t count = cmd.size;
                uint32_t ids[DEBUG_MAX_WATCH_VARS];
                for (uint32_t i = 0; i < count && i < DEBUG_MAX_WATCH_VARS; i++)
                    ids[i] = static_cast<uint32_t>(cmd.value[i]);
                doSetWatchList(ids, count);
                break;
            }
            case PendingCmdType::FORCE:
                doSetForce(cmd.id, cmd.value, cmd.size);
                break;
            case PendingCmdType::UNFORCE:
                doUnforce(cmd.id);
                break;
            case PendingCmdType::CLEAR_FORCES:
                doClearForces();
                break;
        }
        qTail_ = (qTail_ + 1) % DEBUG_MAX_PENDING_COMMANDS;
    }
}

} // namespace rt_plc
