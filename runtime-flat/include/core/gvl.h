#pragma once

#include "constants.h"
#include "types.h"
#include "error_manager.h"
#include <cstdint>
#include <cstring>
#include <cstdio>

namespace rt_plc {

// 缓存行对齐的全局变量表
struct alignas(64) GVL {
    alignas(64) uint8_t memory[GVL_SIZE];

    // RETAIN 区域标记：偏移 [retainStart, retainEnd) 在暖启动时保留
    size_t retainStart = 0;
    size_t retainEnd   = 0;

    // 高水位标记：记录最大写入位置，O(1) usedBytes()
    size_t highWaterMark = 0;

    // 错误管理器指针（用于数组越界等运行时检查）
    ErrorManager* errorMgr = nullptr;

    void clear();

    // 暖启动清零：只清零非 RETAIN 区域
    void clearNonRetain();

    // 设置 RETAIN 区域范围（编译器在 Memory Layout Pass 中计算）
    void setRetainRegion(size_t start, size_t end);

    template<typename T>
    T read(size_t offset) const {
        if (offset + sizeof(T) > GVL_SIZE) {
            fprintf(stderr, "[GVL] read out of bounds: offset=%zu sizeof(T)=%zu GVL_SIZE=%zu\n",
                    offset, sizeof(T), GVL_SIZE);
            return T{};
        }
        T val;
        memcpy(&val, memory + offset, sizeof(T));
        return val;
    }

    template<typename T>
    void write(size_t offset, T val) {
        if (offset + sizeof(T) > GVL_SIZE) {
            fprintf(stderr, "[GVL] write out of bounds: offset=%zu sizeof(T)=%zu GVL_SIZE=%zu\n",
                    offset, sizeof(T), GVL_SIZE);
            return;
        }
        memcpy(memory + offset, &val, sizeof(T));
        size_t end = offset + sizeof(T);
        if (end > highWaterMark) highWaterMark = end;
    }

    template<typename T>
    T* ptr(size_t offset) {
        if (offset + sizeof(T) > GVL_SIZE) {
            fprintf(stderr, "[GVL] ptr out of bounds: offset=%zu sizeof(T)=%zu GVL_SIZE=%zu\n",
                    offset, sizeof(T), GVL_SIZE);
            return nullptr;
        }
        return reinterpret_cast<T*>(memory + offset);
    }

    template<typename T>
    const T* ptr(size_t offset) const {
        if (offset + sizeof(T) > GVL_SIZE) {
            fprintf(stderr, "[GVL] ptr out of bounds: offset=%zu sizeof(T)=%zu GVL_SIZE=%zu\n",
                    offset, sizeof(T), GVL_SIZE);
            return nullptr;
        }
        return reinterpret_cast<const T*>(memory + offset);
    }

    // 安全数组访问（Flat 模式数组越界检查）
    template<typename T>
    T& safeArrayAt(size_t offset, size_t index, size_t count,
                   uint32_t pouId = 0, uint32_t line = 0, TIME sysTime = 0) {
        T* base = ptr<T>(offset);
        if (base == nullptr) {
            static T fallback{};
            return fallback;
        }
        if (index >= count) {
            if (errorMgr != nullptr) {
                errorMgr->report(ErrorCode::ARRAY_OUT_OF_BOUNDS, pouId, line,
                                "array index out of bounds", sysTime,
                                (int64_t)index, (int64_t)count);
            }
            return base[count - 1];
        }
        return base[index];
    }

    // 使用量统计 — O(1) 查询
    size_t usedBytes() const {
        return highWaterMark;
    }
};

} // namespace rt_plc
