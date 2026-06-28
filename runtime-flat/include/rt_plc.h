/**
 * rt_plc.h — 实时 PLC 运行时（运行时基础设施）
 *
 * 类型定义、类型转换、字符串操作等已提取到 core/types.h。
 * 本文件仅包含运行时基础设施：ProcessImage、TCI、
 * 安全运算宏、功能块实例、Runtime 等。
 */
#ifndef RT_PLC_H
#define RT_PLC_H

#include "core/types.h"
#include "core/error_manager.h"
#include "core/platform.h"

#if !defined(RT_PLATFORM_BARE_METAL)
#include <functional>
#include <thread>
#include <atomic>
#endif

namespace rt_plc {

// ═══════════════════════════════════════════════════════
// 过程映像（Process Image）
// ═══════════════════════════════════════════════════════

#if !defined(PROCESS_IMAGE_SIZE_OVERRIDE)
constexpr size_t PROCESS_IMAGE_SIZE = 65536;
#else
constexpr size_t PROCESS_IMAGE_SIZE = PROCESS_IMAGE_SIZE_OVERRIDE;
#endif

#if defined(RT_PLATFORM_BARE_METAL)
struct ProcessImage {
    uint8_t inputs[PROCESS_IMAGE_SIZE];
    uint8_t outputs[PROCESS_IMAGE_SIZE];
#else
struct alignas(64) ProcessImage {
    alignas(64) uint8_t inputs[PROCESS_IMAGE_SIZE];
    alignas(64) uint8_t outputs[PROCESS_IMAGE_SIZE];
#endif

    void clearInputs()  { memset(inputs, 0, PROCESS_IMAGE_SIZE); }
    void clearOutputs() { memset(outputs, 0, PROCESS_IMAGE_SIZE); }

    template<typename T>
    T readInput(size_t offset) const {
        if (offset + sizeof(T) > PROCESS_IMAGE_SIZE) {
            RT_LOG_ERR("[ProcessImage] readInput out of bounds: offset=%zu sizeof(T)=%zu\n",
                    offset, sizeof(T));
            return T{};
        }
        T val;
        memcpy(&val, inputs + offset, sizeof(T));
        return val;
    }

    template<typename T>
    void writeInput(size_t offset, T val) {
        if (offset + sizeof(T) > PROCESS_IMAGE_SIZE) {
            RT_LOG_ERR("[ProcessImage] writeInput out of bounds: offset=%zu sizeof(T)=%zu\n",
                    offset, sizeof(T));
            return;
        }
        memcpy(inputs + offset, &val, sizeof(T));
    }

    template<typename T>
    void writeOutput(size_t offset, T val) {
        if (offset + sizeof(T) > PROCESS_IMAGE_SIZE) {
            RT_LOG_ERR("[ProcessImage] writeOutput out of bounds: offset=%zu sizeof(T)=%zu\n",
                    offset, sizeof(T));
            return;
        }
        memcpy(outputs + offset, &val, sizeof(T));
    }

    template<typename T>
    T readOutput(size_t offset) const {
        if (offset + sizeof(T) > PROCESS_IMAGE_SIZE) {
            RT_LOG_ERR("[ProcessImage] readOutput out of bounds: offset=%zu sizeof(T)=%zu\n",
                    offset, sizeof(T));
            return T{};
        }
        T val;
        memcpy(&val, outputs + offset, sizeof(T));
        return val;
    }

    BOOL readInputBit(size_t byteOff, int bitOff) const {
        if (byteOff >= PROCESS_IMAGE_SIZE || bitOff < 0 || bitOff > 7) {
            RT_LOG_ERR("[ProcessImage] readInputBit out of bounds: byteOff=%zu bitOff=%d\n",
                    byteOff, bitOff);
            return FALSE;
        }
        return (inputs[byteOff] & (1 << bitOff)) ? TRUE : FALSE;
    }

    void writeInputBit(size_t byteOff, int bitOff, BOOL val) {
        if (byteOff >= PROCESS_IMAGE_SIZE || bitOff < 0 || bitOff > 7) {
            RT_LOG_ERR("[ProcessImage] writeInputBit out of bounds: byteOff=%zu bitOff=%d\n",
                    byteOff, bitOff);
            return;
        }
        if (val) inputs[byteOff] |=  (1 << bitOff);
        else     inputs[byteOff] &= ~(1 << bitOff);
    }

    void writeOutputBit(size_t byteOff, int bitOff, BOOL val) {
        if (byteOff >= PROCESS_IMAGE_SIZE || bitOff < 0 || bitOff > 7) {
            RT_LOG_ERR("[ProcessImage] writeOutputBit out of bounds: byteOff=%zu bitOff=%d\n",
                    byteOff, bitOff);
            return;
        }
        if (val) outputs[byteOff] |=  (1 << bitOff);
        else     outputs[byteOff] &= ~(1 << bitOff);
    }

    BOOL readOutputBit(size_t byteOff, int bitOff) const {
        if (byteOff >= PROCESS_IMAGE_SIZE || bitOff < 0 || bitOff > 7) {
            RT_LOG_ERR("[ProcessImage] readOutputBit out of bounds: byteOff=%zu bitOff=%d\n",
                    byteOff, bitOff);
            return FALSE;
        }
        return (outputs[byteOff] & (1 << bitOff)) ? TRUE : FALSE;
    }
};


// ─── I/O 地址宏 ───

#define PLC_IB(b)      ((size_t)(b))
#define PLC_IW(b)      ((size_t)(b))
#define PLC_ID(b)      ((size_t)(b))
#define PLC_IL(b)      ((size_t)(b))
#define PLC_IX(b, bit) ((size_t)(b)), ((int)(bit))

#define PLC_QB(b)      ((size_t)(b))
#define PLC_QW(b)      ((size_t)(b))
#define PLC_QD(b)      ((size_t)(b))
#define PLC_QL(b)      ((size_t)(b))
#define PLC_QX(b, bit) ((size_t)(b)), ((int)(bit))

// ─── 过程映像读写便捷宏（编译器生成代码用） ───

#define PI_READ_INPUT(img, type, offset)       ((img).readInput<type>(offset))
#define PI_WRITE_INPUT(img, type, offset, val) ((img).writeInput<type>((offset), (val)))
#define PI_READ_OUTPUT(img, type, offset)      ((img).readOutput<type>(offset))
#define PI_WRITE_OUTPUT(img, type, offset, val) ((img).writeOutput<type>((offset), (val)))

#define PI_READ_INPUT_BIT(img, b, bit)         ((img).readInputBit((b), (bit)))
#define PI_WRITE_INPUT_BIT(img, b, bit, val)   ((img).writeInputBit((b), (bit), (val)))
#define PI_READ_OUTPUT_BIT(img, b, bit)        ((img).readOutputBit((b), (bit)))
#define PI_WRITE_OUTPUT_BIT(img, b, bit, val)  ((img).writeOutputBit((b), (bit), (val)))


// ─── TCI：硬件 I/O 抽象接口 ───

struct TCI {
    virtual ~TCI() = default;
    virtual void syncInputs(ProcessImage& img) = 0;
    virtual void syncOutputs(ProcessImage& img) = 0;
};

// ─── CompositeTCI：组合多个 TCI（如 GPIO + EtherCAT） ───

struct CompositeTCI : public TCI {
    static constexpr int MAX_TCI = 4;
    TCI* list[MAX_TCI] = {};
    int count_ = 0;

    void add(TCI* t) { if (count_ < MAX_TCI) list[count_++] = t; }
    int count() const { return count_; }

    void syncInputs(ProcessImage& img) override {
        for (int i = 0; i < count_; i++) list[i]->syncInputs(img);
    }
    void syncOutputs(ProcessImage& img) override {
        for (int i = 0; i < count_; i++) list[i]->syncOutputs(img);
    }
};


// ═══════════════════════════════════════════════════════
// 安全运算宏（依赖 ErrorManager）
// ═══════════════════════════════════════════════════════

#define PLC_DIV(a, b, em, pouId, line, sysT) \
    ((em).safeDiv((a), (b), (pouId), (line), (sysT)))
#define PLC_MOD(a, b, em, pouId, line, sysT) \
    ((em).safeMod((a), (b), (pouId), (line), (sysT)))
#define PLC_ADD(a, b, em, pouId, line, sysT) \
    ((em).safeAdd((a), (b), (pouId), (line), (sysT)))
#define PLC_SUB(a, b, em, pouId, line, sysT) \
    ((em).safeSub((a), (b), (pouId), (line), (sysT)))
#define PLC_MUL(a, b, em, pouId, line, sysT) \
    ((em).safeMul((a), (b), (pouId), (line), (sysT)))
#define PLC_ARRAY_AT_SAFE(arr, idx, lower, em, pouId, line, sysT) \
    ((em).safeArrayAt((arr), (size_t)((idx) - (lower)), (pouId), (line), (sysT)))


// ═══════════════════════════════════════════════════════
// 简单单扫描运行时（向后兼容）
// ═══════════════════════════════════════════════════════

typedef void (*RuntimeScanFunc)(void);

struct Runtime {
    ProcessImage image;
    TCI*         tci            = nullptr;
    uint64_t     cycleCount     = 0;
    uint32_t     cycleTimeUs    = 10000;
    bool         running        = false;

    void setCycleTime(uint32_t us) { cycleTimeUs = us; }
    void setTCI(TCI* t) { tci = t; }

#if !defined(RT_PLATFORM_BARE_METAL)
    void scanOnce(std::function<void()> userProgram) {
        if (tci) tci->syncInputs(image);
        userProgram();
        if (tci) tci->syncOutputs(image);
        cycleCount++;
    }

    void run(std::function<void()> userProgram) {
        running = true;
        while (running) {
            auto start = std::chrono::steady_clock::now();
            scanOnce(userProgram);
            auto elapsed = std::chrono::steady_clock::now() - start;
            auto sleepUs = std::chrono::microseconds(cycleTimeUs) -
                           std::chrono::duration_cast<std::chrono::microseconds>(elapsed);
            if (sleepUs.count() > 0) {
                std::this_thread::sleep_for(sleepUs);
            }
        }
    }
#else
    void scanOnce(RuntimeScanFunc userProgram) {
        if (tci) tci->syncInputs(image);
        if (userProgram) userProgram();
        if (tci) tci->syncOutputs(image);
        cycleCount++;
    }

    void run(RuntimeScanFunc userProgram) {
        running = true;
        while (running) {
            int64_t start = platform::steadyUs();
            scanOnce(userProgram);
            int64_t elapsed = platform::steadyUs() - start;
            int64_t sleep = (int64_t)cycleTimeUs - elapsed;
            if (sleep > 0) {
                platform::sleepUs(sleep);
            }
        }
    }
#endif

    void stop() { running = false; }
};


// ═══════════════════════════════════════════════════════
// 互斥锁（任务间共享变量保护）
// ═══════════════════════════════════════════════════════

// PLC 通常是单线程扫描，但在多任务（多 POU 同时执行）场景下，
// 需要保护共享资源。这里提供轻量级的 spinlock。
//
// 用法（编译器为 VAR_ACCESS 块生成）：
//   plc_lock lock;
//   lock.lock();
//   // ... 访问共享变量 ...
//   lock.unlock();
//
// 或用 RAII 包装：
//   { plc_lock_guard g(lock); ... }

#if defined(RT_PLATFORM_BARE_METAL)

// 裸机：单线程无竞争，spinlock 为空操作
// 多核裸机可替换为 LDREX/STREX 实现
struct plc_lock {
    void lock() {}
    void unlock() {}
    bool tryLock() { return true; }
};

#else

struct plc_lock {
    std::atomic_flag flag = ATOMIC_FLAG_INIT;

    void lock() {
        while (flag.test_and_set(std::memory_order_acquire)) {
        }
    }

    void unlock() {
        flag.clear(std::memory_order_release);
    }

    bool tryLock() {
        return !flag.test_and_set(std::memory_order_acquire);
    }
};

#endif

struct plc_lock_guard {
    plc_lock& lock_;
    plc_lock_guard(plc_lock& l) : lock_(l) { lock_.lock(); }
    ~plc_lock_guard() { lock_.unlock(); }
    plc_lock_guard(const plc_lock_guard&) = delete;
    plc_lock_guard& operator=(const plc_lock_guard&) = delete;
};


} // namespace rt_plc

#endif // RT_PLC_H
