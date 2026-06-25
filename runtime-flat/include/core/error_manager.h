#pragma once

#include "types.h"
#include <cstdint>
#include <cstring>
#include <cstdio>

namespace rt_plc {

constexpr int MAX_ERROR_LOG = 32;

struct ErrorEntry {
    ErrorCode   code      = ErrorCode::NONE;
    uint32_t    pouId     = 0;
    uint32_t    lineNo    = 0;
    TIME        timestamp = 0;
    int64_t     operandA  = 0;
    int64_t     operandB  = 0;
    char        message[64] = {0};
};

typedef void (*ErrorHandler)(const ErrorEntry& entry);

struct ErrorManager {
    ErrorEntry  log[MAX_ERROR_LOG];
    int         logIndex    = 0;
    int         totalCount  = 0;
    ErrorCode   lastError   = ErrorCode::NONE;
    bool        fatalMode   = false;
    ErrorHandler handler    = nullptr;

    void setHandler(ErrorHandler h) { handler = h; }

    void report(ErrorCode code, uint32_t pouId, uint32_t line,
                const char* msg, TIME sysTime,
                int64_t opA = 0, int64_t opB = 0) {
        ErrorEntry& e = log[logIndex % MAX_ERROR_LOG];
        e.code      = code;
        e.pouId     = pouId;
        e.lineNo    = line;
        e.timestamp = sysTime;
        e.operandA  = opA;
        e.operandB  = opB;
        strncpy(e.message, msg, sizeof(e.message) - 1);
        e.message[sizeof(e.message) - 1] = '\0';

        logIndex++;
        totalCount++;
        lastError = code;

        if (code == ErrorCode::DIV_BY_ZERO ||
            code == ErrorCode::ARRAY_OUT_OF_BOUNDS ||
            code == ErrorCode::NULL_POINTER ||
            code == ErrorCode::STACK_OVERFLOW) {
            fatalMode = true;
        }

        if (handler) handler(e);
    }

    void reset() {
        logIndex   = 0;
        totalCount = 0;
        lastError  = ErrorCode::NONE;
        fatalMode  = false;
    }

    template<typename T>
    T safeDiv(T a, T b, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (b == (T)0) {
            report(ErrorCode::DIV_BY_ZERO, pouId, line, "division by zero", sysTime,
                   (int64_t)a, (int64_t)b);
            return (T)0;
        }
        return a / b;
    }

    float safeDiv(float a, float b, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (b == 0.0f) {
            int64_t aBits, bBits;
            memcpy(&aBits, &a, sizeof(float));
            memcpy(&bBits, &b, sizeof(float));
            report(ErrorCode::DIV_BY_ZERO, pouId, line, "float division by zero", sysTime,
                   aBits, bBits);
            return 0.0f;
        }
        return a / b;
    }

    double safeDiv(double a, double b, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (b == 0.0) {
            int64_t aBits, bBits;
            memcpy(&aBits, &a, sizeof(double));
            memcpy(&bBits, &b, sizeof(double));
            report(ErrorCode::DIV_BY_ZERO, pouId, line, "lreal division by zero", sysTime,
                   aBits, bBits);
            return 0.0;
        }
        return a / b;
    }

    template<typename T, size_t N>
    T& safeArrayAt(T (&arr)[N], size_t index, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (index >= N) {
            report(ErrorCode::ARRAY_OUT_OF_BOUNDS, pouId, line, "array index out of bounds", sysTime,
                   (int64_t)index, (int64_t)N);
            return arr[N - 1];
        }
        return arr[index];
    }

    template<typename T>
    T& safeArrayAt(T* ptr, size_t index, size_t count, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (index >= count) {
            report(ErrorCode::ARRAY_OUT_OF_BOUNDS, pouId, line, "array index out of bounds", sysTime,
                   (int64_t)index, (int64_t)count);
            return ptr[count - 1];
        }
        return ptr[index];
    }

    template<typename T>
    T safeMod(T a, T b, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (b == (T)0) {
            report(ErrorCode::DIV_BY_ZERO, pouId, line, "MOD by zero", sysTime,
                   (int64_t)a, (int64_t)b);
            return (T)0;
        }
        return a % b;
    }

    template<typename T>
    T safeAdd(T a, T b, uint32_t pouId, uint32_t line, TIME sysTime) {
        T result = a + b;
        if ((b > 0 && result < a) || (b < 0 && result > a)) {
            report(ErrorCode::INT_OVERFLOW, pouId, line, "integer overflow in addition", sysTime,
                   (int64_t)a, (int64_t)b);
        }
        return result;
    }

    template<typename T>
    T safeSub(T a, T b, uint32_t pouId, uint32_t line, TIME sysTime) {
        T result = a - b;
        if ((b > 0 && result > a) || (b < 0 && result < a)) {
            report(ErrorCode::INT_OVERFLOW, pouId, line, "integer overflow in subtraction", sysTime,
                   (int64_t)a, (int64_t)b);
        }
        return result;
    }

    template<typename T>
    T safeMul(T a, T b, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (a == 0 || b == 0) return 0;
        T result = a * b;
        if (result / a != b) {
            report(ErrorCode::INT_OVERFLOW, pouId, line, "integer overflow in multiplication", sysTime,
                   (int64_t)a, (int64_t)b);
        }
        return result;
    }

    void printLog() const {
        printf("=== Error Log (%d total) ===\n", totalCount);
        int cnt = (totalCount < MAX_ERROR_LOG) ? totalCount : MAX_ERROR_LOG;
        for (int i = 0; i < cnt; i++) {
            int idx = (logIndex - cnt + i + MAX_ERROR_LOG) % MAX_ERROR_LOG;
            const ErrorEntry& e = log[idx];
            printf("  [%lld] code=%d pou=%u line=%u opA=%lld opB=%lld msg='%s'\n",
                   (long long)e.timestamp, (int)e.code, e.pouId, e.lineNo,
                   (long long)e.operandA, (long long)e.operandB, e.message);
        }
        printf("===========================\n");
    }

    int count() const { return totalCount; }
    bool isFatal() const { return fatalMode; }
};

} // namespace rt_plc
