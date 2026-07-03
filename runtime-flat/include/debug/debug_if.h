#pragma once

#include "core/gvl.h"
#include "core/constants.h"
#include "core/types.h"
#include "core/diag.h"
#include "rt_plc.h"
#include <cstdint>
#include <cstring>
#include <atomic>

namespace rt_plc {

// ─── 枚举 ───

enum class DebugStorage : uint8_t {
    GVL,
    INPUT,
    OUTPUT,
    MEMORY,
    RETAIN
};

enum class DebugType : uint8_t {
    BOOL,
    SINT,
    INT,
    DINT,
    LINT,
    USINT,
    UINT,
    UDINT,
    ULINT,
    REAL,
    LREAL,
    BYTE,
    WORD,
    DWORD,
    LWORD,
    STRING,
    TIME
};

enum class DebugAccess : uint8_t {
    READ_ONLY,
    READ_WRITE,
    FORCEABLE
};

enum class ForcePhase : uint8_t {
    PRE_LOGIC,    // syncInputs 后、LOGIC_SOLVE 前
    POST_LOGIC    // LOGIC_SOLVE 后、syncOutputs 前
};

// ─── 数据结构 ───

struct DebugVar {
    uint32_t     id;           // 4
    DebugStorage storage;      // 1
    uint8_t      bitOffset;    // 1, 0xFF = 非 bit 变量
    DebugType    type;         // 1
    DebugAccess  access;       // 1
    uint32_t     offset;       // 4
    uint16_t     size;         // 2
    uint16_t     count;        // 2  (数组元素数, 标量=1)
};                             // 总 16 字节

struct DebugSample {
    uint32_t  id;
    uint16_t  size;
    uint8_t   value[8];       // 标量最大 8 字节 (LREAL/LINT)
};

struct DebugSnapshot {
    uint64_t      cycle;
    TIME          systemTime;
    uint32_t      sampleCount;
    DebugSample   samples[DEBUG_MAX_WATCH_VARS];
    uint64_t      totalTicks;
    TIME          lastScanTime;
    TIME          maxScanTime;
    SystemState   systemState;
};

// ─── Pending Command Queue ───

#ifndef DEBUG_MAX_PENDING_COMMANDS
#define DEBUG_MAX_PENDING_COMMANDS 16
#endif

enum class PendingCmdType : uint8_t {
    SET_WATCH,
    FORCE,
    UNFORCE,
    CLEAR_FORCES
};

struct PendingCommand {
    PendingCmdType type;
    uint32_t       id;
    uint8_t        value[8];
    uint16_t       size;
};

// ─── Force Entry ───

#ifndef DEBUG_MAX_FORCES
#define DEBUG_MAX_FORCES 32
#endif

struct ForceEntry {
    uint32_t     id;
    uint32_t     offset;
    uint16_t     size;
    uint8_t      value[8];
    bool         active;
};

// ─── DebugEngine ───

class DebugEngine {
public:
    DebugEngine();

    // 初始化
    void init(const DebugVar* vars, uint32_t count, const uint8_t* buildId);

    // bindMemory() 存的指针供 transport 线程的 readMemory() 使用
    // applyForces / publishSnapshot 接收引用供 Scheduler tick 线程调用
    void bindMemory(GVL* gvl, ProcessImage* image);

    // ── Metadata ──
    void hello(uint8_t outBuildId[16], uint32_t& outVarCount) const;
    const DebugVar* findVar(uint32_t id) const;
    const DebugVar* vars() const { return vars_; }
    uint32_t varCount() const { return varCount_; }

    // ── Snapshot (watch list) ──
    bool requestSetWatchList(const uint32_t* ids, uint32_t count);
    uint32_t watchCount() const { return watchCount_; }

    // Scheduler::tick() 末尾调用
    void publishSnapshot(const GVL& gvl,
                         const ProcessImage& io,
                         uint64_t cycle,
                         TIME systemTime,
                         const DiagStats& diag,
                         SystemState state);

    const DebugSnapshot& latestSnapshot() const;

    // transport 线程调用：从 snapshot 读变量
    bool readVar(uint32_t id, uint8_t* outValue, uint16_t& outSize) const;

    // transport 线程调用：直接读活内存（标量/数组/raw dump 均可）
    bool readMemory(DebugStorage storage, uint32_t offset,
                    uint32_t size, uint8_t* out) const;

    // ── Force ──
    bool requestForce(uint32_t id, const uint8_t* value, uint16_t size);
    bool requestUnforce(uint32_t id);
    void requestClearForces();

    // Scheduler::tick() 调用：从 pending queue 提交命令到内部状态
    void applyPendingCommands();

    // Scheduler::tick() 在指定 phase 调用
    void applyForces(GVL& gvl, ProcessImage& io,
                     ForcePhase phase, SystemState state);

    // ── 诊断 ──
    bool isForced(uint32_t id) const;
    uint32_t droppedCommands() const { return droppedCommands_; }

private:
    const DebugVar*  vars_     = nullptr;
    uint32_t         varCount_ = 0;
    const uint8_t*   buildId_  = nullptr;

    // bindMemory 存的指针（供 transport 线程 readMemory 用）
    GVL*          boundGVL_  = nullptr;
    ProcessImage* boundIO_   = nullptr;

    // Watch list（内部状态，仅由 applyPendingCommands 修改）
    uint32_t  watchIds_[DEBUG_MAX_WATCH_VARS];
    uint32_t  watchCount_ = 0;

    // 双缓冲快照
    DebugSnapshot   snapBuf_[2];
    std::atomic<int> activeSnapIdx_{0};

    // Pending command queue（transport 线程写入，applyPendingCommands 消费）
    PendingCommand  pendingQ_[DEBUG_MAX_PENDING_COMMANDS];
    uint32_t        qHead_ = 0;
    uint32_t        qTail_ = 0;
    uint32_t        droppedCommands_ = 0;

    bool pushPendingCommand(const PendingCommand& cmd);

    // Force table（内部状态，仅由 applyPendingCommands 修改）
    ForceEntry  forceTable_[DEBUG_MAX_FORCES];
    int         forceCount_ = 0;

    int  findForceSlot(uint32_t id) const;
    void doSetForce(uint32_t id, const uint8_t* value, uint16_t size);
    void doUnforce(uint32_t id);
    void doClearForces();
    void doSetWatchList(const uint32_t* ids, uint32_t count);
};

} // namespace rt_plc
