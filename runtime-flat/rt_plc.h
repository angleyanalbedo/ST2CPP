/**
 * rt_plc.h — 实时 PLC 运行时（类型 + 功能块 + 内置函数 + 基础框架）
 *
 * 面向 ST→C++ 编译器的实时运行时头文件。
 * 设计原则：零运行时开销，变量为原生 C++ 类型，
 * 无堆分配、无 map 查找、无 dynamic_cast。
 *
 * 生成的 C++ 代码直接 #include 此头文件即可。
 */
#ifndef RT_PLC_H
#define RT_PLC_H

#include <cstdint>
#include <cstring>
#include <cstdio>
#include <cmath>
#include <chrono>
#include <thread>
#include <functional>
#include <atomic>

namespace rt_plc {

// ═══════════════════════════════════════════════════════
// 基本类型（IEC 61131-3 数据类型）
// ═══════════════════════════════════════════════════════

typedef int8_t    SINT;      //  8-bit 有符号
typedef int16_t   INT;       // 16-bit 有符号
typedef int32_t   DINT;      // 32-bit 有符号
typedef int64_t   LINT;      // 64-bit 有符号

typedef uint8_t   USINT;     //  8-bit 无符号
typedef uint16_t  UINT;      // 16-bit 无符号
typedef uint32_t  UDINT;     // 32-bit 无符号
typedef uint64_t  ULINT;     // 64-bit 无符号

typedef float     REAL;      // 32-bit 浮点
typedef double    LREAL;     // 64-bit 浮点

// BOOL 用 int8_t 而非 bool，以支持位运算和整数语义
typedef int8_t    BOOL;
#define TRUE  ((BOOL)1)
#define FALSE ((BOOL)0)


// ═══════════════════════════════════════════════════════
// STRING / WSTRING
// ═══════════════════════════════════════════════════════

constexpr size_t STRING_MAX_LEN = 255;

struct STRING {
    char data[STRING_MAX_LEN + 1];

    STRING() { data[0] = '\0'; }
    STRING(const char* s) {
        strncpy(data, s, STRING_MAX_LEN);
        data[STRING_MAX_LEN] = '\0';
    }

    bool operator==(const STRING& o) const { return strcmp(data, o.data) == 0; }
    bool operator!=(const STRING& o) const { return strcmp(data, o.data) != 0; }
};

struct WSTRING {
    wchar_t data[STRING_MAX_LEN + 1];
    WSTRING() { data[0] = L'\0'; }
};


// ═══════════════════════════════════════════════════════
// TIME / DATE / TIME_OF_DAY / DATE_AND_TIME
// ═══════════════════════════════════════════════════════

// TIME: 微秒精度时间间隔（有符号，支持负差值）
typedef int64_t TIME;

// 辅助构造（ST 的 T#100ms 由编译器翻译成这些调用）
inline TIME T_us(int64_t us)  { return us; }
inline TIME T_ms(int64_t ms)  { return ms * 1000; }
inline TIME T_s(int64_t s)    { return s * 1000000; }
inline TIME T_min(int64_t m)  { return m * 60000000; }
inline TIME T_h(int64_t h)    { return h * 3600000000LL; }
inline TIME T_d(int64_t d)    { return d * 86400000000LL; }

// TIME 比较辅助
inline BOOL T_eq(TIME a, TIME b) { return a == b ? TRUE : FALSE; }
inline BOOL T_ne(TIME a, TIME b) { return a != b ? TRUE : FALSE; }
inline BOOL T_lt(TIME a, TIME b) { return a <  b ? TRUE : FALSE; }
inline BOOL T_le(TIME a, TIME b) { return a <= b ? TRUE : FALSE; }
inline BOOL T_gt(TIME a, TIME b) { return a >  b ? TRUE : FALSE; }
inline BOOL T_ge(TIME a, TIME b) { return a >= b ? TRUE : FALSE; }

// DATE: 自 1970-01-01 的天数
typedef int32_t DATE;

// TIME_OF_DAY: 自午夜起的微秒数 [0, 86400000000)
typedef int64_t TIME_OF_DAY;

// DATE_AND_TIME: Unix 时间戳（微秒）
typedef int64_t DATE_AND_TIME;

// DATE 构造：D#2025-01-15 → 编译器翻译为 DATE_make(2025,1,15)
inline DATE DATE_make(int year, int month, int day) {
    // 简化的 Julian Day 计算（足够 1970-2100 范围）
    int a = (14 - month) / 12;
    int y = year + 4800 - a;
    int m = month + 12 * a - 3;
    int jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    return (DATE)(jdn - 2440588);  // 相对于 1970-01-01
}

// TOD 构造：TOD#14:30:00 → 编译器翻译为 TOD_make(14,30,0,0)
inline TIME_OF_DAY TOD_make(int hour, int minute, int second, int ms = 0) {
    return (TIME_OF_DAY)hour * 3600000000LL
         + (TIME_OF_DAY)minute * 60000000LL
         + (TIME_OF_DAY)second * 1000000LL
         + (TIME_OF_DAY)ms * 1000LL;
}

// DT 构造：DT#2025-01-15-14:30:00 → 编译器翻译为 DT_make(...)
inline DATE_AND_TIME DT_make(int year, int month, int day,
                             int hour, int minute, int second) {
    DATE d = DATE_make(year, month, day);
    return (DATE_AND_TIME)d * 86400000000LL + TOD_make(hour, minute, second);
}

// DT 分解
inline DATE DT_to_date(DATE_AND_TIME dt) { return (DATE)(dt / 86400000000LL); }
inline TIME_OF_DAY DT_to_tod(DATE_AND_TIME dt) { return dt % 86400000000LL; }

// NOW(): 返回当前系统时间
// runtimeEpoch 在 Scheduler 初始化时设定，NOW() 返回相对于 epoch 的微秒
inline DATE_AND_TIME NOW() {
    auto now = std::chrono::system_clock::now();
    auto us = std::chrono::duration_cast<std::chrono::microseconds>(
        now.time_since_epoch()).count();
    return (DATE_AND_TIME)us;
}

// 获取挂钟 TIME_OF_DAY
inline TIME_OF_DAY NOW_TOD() {
    auto now = NOW();
    return now % 86400000000LL;
}

// 获取当前 DATE
inline DATE NOW_DATE() {
    auto now = NOW();
    return (DATE)(now / 86400000000LL);
}


// ═══════════════════════════════════════════════════════
// 位串类型（IEC 61131-3）
// ═══════════════════════════════════════════════════════

typedef uint8_t  BYTE;
typedef uint16_t WORD;
typedef uint32_t DWORD;
typedef uint64_t LWORD;


// ═══════════════════════════════════════════════════════
// 位操作（IEC 61131-3 标准运算符）
// ═══════════════════════════════════════════════════════

// AND / OR / XOR / NOT（按位，适用于整数类型）
template<typename T> inline T plc_and(T a, T b)  { return a & b; }
template<typename T> inline T plc_or(T a, T b)   { return a | b; }
template<typename T> inline T plc_xor(T a, T b)  { return a ^ b; }
template<typename T> inline T plc_not(T a)        { return (T)~a; }

// 移位
template<typename T> inline T SHL(T val, INT n) { return (T)(val << n); }
template<typename T> inline T SHR(T val, INT n) { return (T)((unsigned long long)val >> n); }

// 循环移位
inline BYTE ROL(BYTE val, INT n) {
    n = n & 7;
    return (BYTE)((val << n) | (val >> (8 - n)));
}
inline BYTE ROR(BYTE val, INT n) {
    n = n & 7;
    return (BYTE)((val >> n) | (val << (8 - n)));
}


// ═══════════════════════════════════════════════════════
// 过程映像（Process Image）
// ═══════════════════════════════════════════════════════

constexpr size_t PROCESS_IMAGE_SIZE = 65536;  // 64 KB

// 缓存行对齐，避免 false sharing
struct alignas(64) ProcessImage {
    alignas(64) uint8_t inputs[PROCESS_IMAGE_SIZE];
    alignas(64) uint8_t outputs[PROCESS_IMAGE_SIZE];

    void clearInputs()  { memset(inputs, 0, PROCESS_IMAGE_SIZE); }
    void clearOutputs() { memset(outputs, 0, PROCESS_IMAGE_SIZE); }

    // 按偏移读写
    template<typename T>
    T readInput(size_t offset) const {
        T val;
        memcpy(&val, inputs + offset, sizeof(T));
        return val;
    }

    template<typename T>
    void writeOutput(size_t offset, T val) {
        memcpy(outputs + offset, &val, sizeof(T));
    }

    template<typename T>
    T readOutput(size_t offset) const {
        T val;
        memcpy(&val, outputs + offset, sizeof(T));
        return val;
    }

    // 按位访问（%IX0.0 风格）
    BOOL readInputBit(size_t byteOff, int bitOff) const {
        return (inputs[byteOff] & (1 << bitOff)) ? TRUE : FALSE;
    }

    void writeOutputBit(size_t byteOff, int bitOff, BOOL val) {
        if (val) outputs[byteOff] |=  (1 << bitOff);
        else     outputs[byteOff] &= ~(1 << bitOff);
    }
};


// ─── I/O 地址宏 ───
//
// ST 的直接地址 %IX0.0, %QW4, %MD100 等，编译器翻译为偏移量。
// 以下宏让生成的代码可读：
//
//   %IX0.0  → PLC_IX(0, 0)
//   %IB2    → PLC_IB(2)
//   %IW4    → PLC_IW(4)
//   %ID8    → PLC_ID(8)
//   %QX1.3  → PLC_QX(1, 3)
//   %QB0    → PLC_QB(0)
//   %QW2    → PLC_QW(2)
//   %QD4    → PLC_QD(4)

// 输入地址 → 字节偏移
#define PLC_IB(b)      ((size_t)(b))
#define PLC_IW(b)      ((size_t)(b))
#define PLC_ID(b)      ((size_t)(b))
#define PLC_IL(b)      ((size_t)(b))
// 输入位地址 → (字节偏移, 位偏移)
#define PLC_IX(b, bit) ((size_t)(b)), ((int)(bit))

// 输出地址 → 字节偏移
#define PLC_QB(b)      ((size_t)(b))
#define PLC_QW(b)      ((size_t)(b))
#define PLC_QD(b)      ((size_t)(b))
#define PLC_QL(b)      ((size_t)(b))
// 输出位地址
#define PLC_QX(b, bit) ((size_t)(b)), ((int)(bit))

// 编译器生成的读取代码示例：
//   ST:  x := %IX0.3;
//   C++: x = io.readInputBit(PLC_IX(0, 3));
//
//   ST:  y := %IW10;
//   C++: y = io.readInput<INT>(PLC_IW(10));


// ─── TCI：硬件 I/O 抽象接口 ───

struct TCI {
    virtual ~TCI() = default;
    virtual void syncInputs(ProcessImage& img) = 0;
    virtual void syncOutputs(ProcessImage& img) = 0;
};


// ═══════════════════════════════════════════════════════
// 运行时错误处理
// ═══════════════════════════════════════════════════════

// PLC 运行时不应崩溃。所有异常被捕获并转化为错误状态。
// 严重错误（除零、越界）→ 系统进入 ERROR 状态
// 轻微错误（溢出、精度丢失）→ 记录并继续

enum class ErrorCode : uint16_t {
    NONE = 0,
    DIV_BY_ZERO,
    ARRAY_OUT_OF_BOUNDS,
    INT_OVERFLOW,
    FLOAT_OVERFLOW,
    NULL_POINTER,
    STRING_OVERFLOW,
    STACK_OVERFLOW,
    WATCHDOG_TIMEOUT,
    IO_ERROR,
    CONFIG_ERROR,
    USER_ERROR        // 用户自定义（ASSERT 等）
};

constexpr int MAX_ERROR_LOG = 32;  // 环形错误日志

struct ErrorEntry {
    ErrorCode   code      = ErrorCode::NONE;
    uint32_t    pouId     = 0;     // 发生错误的 POU 标识（编译器分配）
    uint32_t    lineNo    = 0;     // ST 源码行号（编译器注入）
    TIME        timestamp = 0;     // 系统时间
    char        message[64] = {0};
};

// 错误处理回调
typedef void (*ErrorHandler)(const ErrorEntry& entry);

struct ErrorManager {
    ErrorEntry  log[MAX_ERROR_LOG];
    int         logIndex    = 0;
    int         totalCount  = 0;
    ErrorCode   lastError   = ErrorCode::NONE;
    bool        fatalMode   = false;   // true = 严重错误已发生，需停机
    ErrorHandler handler    = nullptr;

    void setHandler(ErrorHandler h) { handler = h; }

    // 报告错误（编译器在可能出错的点生成此调用）
    void report(ErrorCode code, uint32_t pouId, uint32_t line,
                const char* msg, TIME sysTime) {
        ErrorEntry& e = log[logIndex % MAX_ERROR_LOG];
        e.code      = code;
        e.pouId     = pouId;
        e.lineNo    = line;
        e.timestamp = sysTime;
        strncpy(e.message, msg, sizeof(e.message) - 1);
        e.message[sizeof(e.message) - 1] = '\0';

        logIndex++;
        totalCount++;
        lastError = code;

        // 严重错误标记
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

    // 安全除法：编译器为所有除法生成此调用
    template<typename T>
    T safeDiv(T a, T b, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (b == (T)0) {
            report(ErrorCode::DIV_BY_ZERO, pouId, line, "division by zero", sysTime);
            return (T)0;  // 返回 0，不崩溃
        }
        return a / b;
    }

    REAL safeDiv(REAL a, REAL b, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (b == 0.0f) {
            report(ErrorCode::DIV_BY_ZERO, pouId, line, "float division by zero", sysTime);
            return 0.0f;
        }
        return a / b;
    }

    LREAL safeDiv(LREAL a, LREAL b, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (b == 0.0) {
            report(ErrorCode::DIV_BY_ZERO, pouId, line, "lreal division by zero", sysTime);
            return 0.0;
        }
        return a / b;
    }

    // 安全数组访问
    template<typename T, size_t N>
    T& safeArrayAt(T (&arr)[N], size_t index, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (index >= N) {
            report(ErrorCode::ARRAY_OUT_OF_BOUNDS, pouId, line, "array index out of bounds", sysTime);
            return arr[N - 1];  // 安全回退
        }
        return arr[index];
    }

    // 安全 MOD
    template<typename T>
    T safeMod(T a, T b, uint32_t pouId, uint32_t line, TIME sysTime) {
        if (b == (T)0) {
            report(ErrorCode::DIV_BY_ZERO, pouId, line, "MOD by zero", sysTime);
            return (T)0;
        }
        return a % b;
    }

    void printLog() const {
        printf("=== Error Log (%d total) ===\n", totalCount);
        int count = (totalCount < MAX_ERROR_LOG) ? totalCount : MAX_ERROR_LOG;
        for (int i = 0; i < count; i++) {
            int idx = (logIndex - count + i + MAX_ERROR_LOG) % MAX_ERROR_LOG;
            const ErrorEntry& e = log[idx];
            printf("  [%lld] code=%d pou=%u line=%u msg='%s'\n",
                   (long long)e.timestamp, (int)e.code, e.pouId, e.lineNo, e.message);
        }
        printf("===========================\n");
    }
};

// 编译器在除法点生成的调用宏（方便代码生成器）
// 用法：PLC_DIV(a, b, ctx)  ctx 是 ErrorContext
#define PLC_DIV(a, b, em, pouId, line, sysT) \
    ((em).safeDiv((a), (b), (pouId), (line), (sysT)))
#define PLC_MOD(a, b, em, pouId, line, sysT) \
    ((em).safeMod((a), (b), (pouId), (line), (sysT)))
#define PLC_ARRAY_AT_SAFE(arr, idx, lower, em, pouId, line, sysT) \
    ((em).safeArrayAt((arr), (size_t)((idx) - (lower)), (pouId), (line), (sysT)))


// ═══════════════════════════════════════════════════════
// ST 内置函数
// ═══════════════════════════════════════════════════════

// ─── 类型转换 ───

// → INT
inline INT   TO_INT(SINT v)    { return (INT)v; }
inline INT   TO_INT(DINT v)    { return (INT)v; }
inline INT   TO_INT(LINT v)    { return (INT)v; }
inline INT   TO_INT(REAL v)    { return (INT)v; }
inline INT   TO_INT(LREAL v)   { return (INT)v; }
inline INT   TO_INT(USINT v)   { return (INT)v; }
inline INT   TO_INT(UINT v)    { return (INT)v; }

// → DINT
inline DINT  TO_DINT(INT v)    { return (DINT)v; }
inline DINT  TO_DINT(LINT v)   { return (DINT)v; }
inline DINT  TO_DINT(REAL v)   { return (DINT)v; }
inline DINT  TO_DINT(LREAL v)  { return (DINT)v; }
inline DINT  TO_DINT(UDINT v)  { return (DINT)v; }

// → LINT
inline LINT  TO_LINT(INT v)    { return (LINT)v; }
inline LINT  TO_LINT(DINT v)   { return (LINT)v; }
inline LINT  TO_LINT(REAL v)   { return (LINT)v; }
inline LINT  TO_LINT(LREAL v)  { return (LINT)v; }
inline LINT  TO_LINT(ULINT v)  { return (LINT)v; }

// → REAL
inline REAL  TO_REAL(INT v)    { return (REAL)v; }
inline REAL  TO_REAL(DINT v)   { return (REAL)v; }
inline REAL  TO_REAL(LINT v)   { return (REAL)v; }
inline REAL  TO_REAL(LREAL v)  { return (REAL)v; }

// → LREAL
inline LREAL TO_LREAL(REAL v)  { return (LREAL)v; }
inline LREAL TO_LREAL(INT v)   { return (LREAL)v; }
inline LREAL TO_LREAL(DINT v)  { return (LREAL)v; }
inline LREAL TO_LREAL(LINT v)  { return (LREAL)v; }

// → SINT / USINT
inline SINT  TO_SINT(INT v)    { return (SINT)v; }
inline SINT  TO_SINT(DINT v)   { return (SINT)v; }
inline USINT TO_USINT(UINT v)  { return (USINT)v; }
inline USINT TO_USINT(INT v)   { return (USINT)v; }

// → UINT / UDINT / ULINT
inline UINT  TO_UINT(INT v)    { return (UINT)v; }
inline UINT  TO_UINT(DINT v)   { return (UINT)v; }
inline UDINT TO_UDINT(DINT v)  { return (UDINT)v; }
inline ULINT TO_ULINT(LINT v)  { return (ULINT)v; }

// → BOOL
inline BOOL  TO_BOOL(INT v)    { return v ? TRUE : FALSE; }
inline BOOL  TO_BOOL(DINT v)   { return v ? TRUE : FALSE; }
inline BOOL  TO_BOOL(LINT v)   { return v ? TRUE : FALSE; }
inline BOOL  TO_BOOL(REAL v)   { return v != 0.0f ? TRUE : FALSE; }
inline BOOL  TO_BOOL(LREAL v)  { return v != 0.0 ? TRUE : FALSE; }

// → STRING
inline STRING TO_STRING(INT v) {
    STRING s;
    snprintf(s.data, STRING_MAX_LEN + 1, "%d", (int)v);
    return s;
}
inline STRING TO_STRING(DINT v) {
    STRING s;
    snprintf(s.data, STRING_MAX_LEN + 1, "%d", (int)v);
    return s;
}
inline STRING TO_STRING(LINT v) {
    STRING s;
    snprintf(s.data, STRING_MAX_LEN + 1, "%lld", (long long)v);
    return s;
}
inline STRING TO_STRING(REAL v) {
    STRING s;
    snprintf(s.data, STRING_MAX_LEN + 1, "%g", (double)v);
    return s;
}
inline STRING TO_STRING(LREAL v) {
    STRING s;
    snprintf(s.data, STRING_MAX_LEN + 1, "%g", (double)v);
    return s;
}
inline STRING TO_STRING(BOOL v) {
    STRING s;
    strncpy(s.data, v ? "TRUE" : "FALSE", STRING_MAX_LEN + 1);
    return s;
}

// ─── 数学函数 ───

inline SINT  ABS(SINT x)   { return x < 0 ? -x : x; }
inline INT   ABS(INT x)    { return x < 0 ? -x : x; }
inline DINT  ABS(DINT x)   { return x < 0 ? -x : x; }
inline LINT  ABS(LINT x)   { return x < 0 ? -x : x; }
inline REAL  ABS(REAL x)   { return x < 0 ? -x : x; }
inline LREAL ABS(LREAL x)  { return x < 0 ? -x : x; }

// SQRT — 使用硬件 FPU 指令（编译器自动内联为 sqrtss/sqrtsd）
inline REAL  SQRT(REAL x)  { return (REAL)std::sqrt((double)x); }
inline LREAL SQRT(LREAL x) { return std::sqrt(x); }

// 幂运算（整数指数）
inline REAL  EXPT(REAL base, DINT exp) {
    REAL result = 1.0f;
    bool neg = exp < 0;
    if (neg) exp = -exp;
    for (DINT i = 0; i < exp; i++) result *= base;
    return neg ? 1.0f / result : result;
}

// LN / LOG — 使用硬件 FPU 指令
inline REAL  LN(REAL x)   { return (REAL)std::log((double)x); }
inline LREAL LN(LREAL x)  { return std::log(x); }
inline REAL  LOG(REAL x)  { return (REAL)std::log10((double)x); }
inline LREAL LOG(LREAL x) { return std::log10(x); }

// MIN / MAX
template<typename T> inline T MIN(T a, T b) { return a < b ? a : b; }
template<typename T> inline T MAX(T a, T b) { return a > b ? a : b; }

// MOD
inline SINT  MOD(SINT a, SINT b)   { return b ? a % b : 0; }
inline INT   MOD(INT a, INT b)     { return b ? a % b : 0; }
inline DINT  MOD(DINT a, DINT b)   { return b ? a % b : 0; }
inline LINT  MOD(LINT a, LINT b)   { return b ? a % b : 0; }

// SEL / MUX / LIMIT
template<typename T>
inline T SEL(BOOL cond, T falseVal, T trueVal) {
    return cond ? trueVal : falseVal;
}

template<typename T>
inline T LIMIT(T minVal, T val, T maxVal) {
    if (val < minVal) return minVal;
    if (val > maxVal) return maxVal;
    return val;
}

// MUX: 根据选择器从 N 个输入中选一个
// 编译器展开为 switch
template<typename T>
inline T MUX(INT selector, T val0, T val1) {
    return selector == 0 ? val0 : val1;
}
template<typename T>
inline T MUX(INT selector, T val0, T val1, T val2, T val3) {
    switch (selector) {
        case 0: return val0;
        case 1: return val1;
        case 2: return val2;
        default: return val3;
    }
}


// ═══════════════════════════════════════════════════════
// 边沿检测
// ═══════════════════════════════════════════════════════

struct R_TRIG {
    BOOL lastState = FALSE;
    BOOL operator()(BOOL input) {
        BOOL result = (input && !lastState) ? TRUE : FALSE;
        lastState = input;
        return result;
    }
};

struct F_TRIG {
    BOOL lastState = TRUE;
    BOOL operator()(BOOL input) {
        BOOL result = (!input && lastState) ? TRUE : FALSE;
        lastState = input;
        return result;
    }
};


// ═══════════════════════════════════════════════════════
// 标准功能块（定时器 + 计数器）
// ═══════════════════════════════════════════════════════

// TON: 接通延时
struct TON {
    BOOL  input   = FALSE;
    TIME  preset  = 0;
    TIME  elapsed = 0;
    BOOL  output  = FALSE;

    void update(BOOL IN, TIME PT, TIME cycleTimeUs) {
        input  = IN;
        preset = PT;
        if (IN) {
            elapsed += cycleTimeUs;
            if (elapsed >= preset) {
                elapsed = preset;
                output = TRUE;
            }
        } else {
            elapsed = 0;
            output  = FALSE;
        }
    }
};

// TOF: 断开延时
struct TOF {
    BOOL  input   = TRUE;
    TIME  preset  = 0;
    TIME  elapsed = 0;
    BOOL  output  = FALSE;

    void update(BOOL IN, TIME PT, TIME cycleTimeUs) {
        if (IN) {
            elapsed = 0;
            output  = TRUE;
        } else {
            elapsed += cycleTimeUs;
            if (elapsed >= preset) {
                elapsed = preset;
                output = FALSE;
            }
        }
    }
};

// TP: 脉冲定时器
struct TP {
    BOOL  lastInput = FALSE;
    TIME  preset    = 0;
    TIME  elapsed   = 0;
    BOOL  output    = FALSE;

    void update(BOOL IN, TIME PT, TIME cycleTimeUs) {
        preset = PT;
        if (IN && !lastInput && !output) {
            output  = TRUE;
            elapsed = 0;
        }
        lastInput = IN;
        if (output) {
            elapsed += cycleTimeUs;
            if (elapsed >= PT) {
                elapsed = PT;
                output  = FALSE;
            }
        } else if (!IN) {
            elapsed = 0;
        }
    }
};

// CTU: 递增计数器
struct CTU {
    BOOL  lastCU  = FALSE;
    INT   count   = 0;
    BOOL  output  = FALSE;

    void update(BOOL CU, BOOL RESET, INT PV) {
        if (RESET) {
            count = 0;
        } else if (CU && !lastCU) {
            count++;
        }
        lastCU = CU;
        output = (count >= PV) ? TRUE : FALSE;
    }
};

// CTD: 递减计数器
struct CTD {
    BOOL  lastCD  = FALSE;
    INT   count   = 0;
    BOOL  output  = FALSE;

    void update(BOOL CD, BOOL LOAD, INT PV) {
        if (LOAD) {
            count = PV;
        } else if (CD && !lastCD) {
            count--;
        }
        lastCD = CD;
        output = (count <= 0) ? TRUE : FALSE;
    }
};

// CTUD: 递增/递减计数器
struct CTUD {
    BOOL  lastCU  = FALSE;
    BOOL  lastCD  = FALSE;
    INT   count   = 0;
    BOOL  QU      = FALSE;
    BOOL  QD      = FALSE;

    void update(BOOL CU, BOOL CD, BOOL RESET, BOOL LOAD, INT PV) {
        if (RESET) {
            count = 0;
        } else if (LOAD) {
            count = PV;
        } else {
            if (CU && !lastCU) count++;
            if (CD && !lastCD) count--;
        }
        lastCU = CU;
        lastCD = CD;
        QU = (count >= PV) ? TRUE : FALSE;
        QD = (count <= 0)  ? TRUE : FALSE;
    }
};


// ═══════════════════════════════════════════════════════
// 复合类型
// ═══════════════════════════════════════════════════════

// ─── ARRAY ───

template<typename T, size_t N>
struct PLC_Array {
    T elements[N];

    PLC_Array() {
        for (size_t i = 0; i < N; i++) elements[i] = T{};
    }

    PLC_Array(T init) {
        for (size_t i = 0; i < N; i++) elements[i] = init;
    }

    T& at(size_t index) {
        if (index >= N) {
            fprintf(stderr, "ARRAY out of bounds: index %zu, size %zu\n", index, N);
            index = N - 1;
        }
        return elements[index];
    }

    const T& at(size_t index) const {
        if (index >= N) {
            fprintf(stderr, "ARRAY out of bounds: index %zu, size %zu\n", index, N);
            index = N - 1;
        }
        return elements[index];
    }

    T& operator[](size_t index) { return elements[index]; }
    const T& operator[](size_t index) const { return elements[index]; }

    PLC_Array& operator=(const PLC_Array& other) {
        if (this != &other) {
            for (size_t i = 0; i < N; i++) elements[i] = other.elements[i];
        }
        return *this;
    }

    bool operator==(const PLC_Array& other) const {
        for (size_t i = 0; i < N; i++) {
            if (!(elements[i] == other.elements[i])) return false;
        }
        return true;
    }

    constexpr size_t size() const { return N; }

    void fill(T val) {
        for (size_t i = 0; i < N; i++) elements[i] = val;
    }
};

#define PLC_ARRAY_AT(arr, idx, lower) ((arr).at((idx) - (lower)))


// ─── SUBRANGE ───

template<typename T, T Low, T High>
struct PLC_Subrange {
    T value;

    PLC_Subrange() : value(Low) {}
    PLC_Subrange(T v) : value(clamp(v)) {}

    PLC_Subrange& operator=(T v) {
        value = clamp(v);
        return *this;
    }

    explicit operator T() const { return value; }

    T clamp(T v) const {
        if (v < Low) return Low;
        if (v > High) return High;
        return v;
    }

    PLC_Subrange operator+(T rhs) const { return PLC_Subrange(value + rhs); }
    PLC_Subrange operator-(T rhs) const { return PLC_Subrange(value - rhs); }
    PLC_Subrange operator*(T rhs) const { return PLC_Subrange(value * rhs); }
    PLC_Subrange operator/(T rhs) const { return PLC_Subrange(rhs ? value / rhs : value); }

    bool operator==(T rhs) const { return value == rhs; }
    bool operator!=(T rhs) const { return value != rhs; }
    bool operator<(T rhs)  const { return value < rhs; }
    bool operator<=(T rhs) const { return value <= rhs; }
    bool operator>(T rhs)  const { return value > rhs; }
    bool operator>=(T rhs) const { return value >= rhs; }
};


// ─── ENUM ───
// 编译器直接生成 C++ enum class，不需要运行时支持
#define PLC_ENUM_COUNT(enumType) (static_cast<int>(enumType::_COUNT))


// ─── STRUCT ───
// 编译器直接生成 C++ struct

inline void str_assign(STRING& dst, const char* src) {
    strncpy(dst.data, src, STRING_MAX_LEN);
    dst.data[STRING_MAX_LEN] = '\0';
}

inline void str_assign(STRING& dst, const STRING& src) {
    memcpy(dst.data, src.data, STRING_MAX_LEN + 1);
}


// ─── 字符串操作（IEC 61131-3 标准） ───

// CONCAT: 拼接两个字符串
inline STRING CONCAT(const STRING& a, const STRING& b) {
    STRING result;
    size_t lenA = strlen(a.data);
    size_t remain = STRING_MAX_LEN - lenA;
    memcpy(result.data, a.data, lenA);
    strncpy(result.data + lenA, b.data, remain);
    result.data[STRING_MAX_LEN] = '\0';
    return result;
}

// LEN: 字符串长度
inline DINT LEN(const STRING& s) {
    return (DINT)strlen(s.data);
}

// EQ / NE / LT / LE / GT / GE: 字符串比较
inline BOOL STR_EQ(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) == 0 ? TRUE : FALSE; }
inline BOOL STR_NE(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) != 0 ? TRUE : FALSE; }
inline BOOL STR_LT(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) <  0 ? TRUE : FALSE; }
inline BOOL STR_LE(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) <= 0 ? TRUE : FALSE; }
inline BOOL STR_GT(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) >  0 ? TRUE : FALSE; }
inline BOOL STR_GE(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) >= 0 ? TRUE : FALSE; }

// LEFT: 取左边 L 个字符
inline STRING LEFT(const STRING& s, DINT L) {
    STRING result;
    DINT len = (DINT)strlen(s.data);
    if (L > len) L = len;
    if (L < 0) L = 0;
    memcpy(result.data, s.data, L);
    result.data[L] = '\0';
    return result;
}

// RIGHT: 取右边 L 个字符
inline STRING RIGHT(const STRING& s, DINT L) {
    STRING result;
    DINT len = (DINT)strlen(s.data);
    if (L > len) L = len;
    if (L < 0) L = 0;
    memcpy(result.data, s.data + (len - L), L);
    result.data[L] = '\0';
    return result;
}

// MID: 取中间 L 个字符，从位置 P 开始（1-based）
inline STRING MID(const STRING& s, DINT L, DINT P) {
    STRING result;
    DINT len = (DINT)strlen(s.data);
    if (P < 1) P = 1;
    if (P > len) { result.data[0] = '\0'; return result; }
    DINT start = P - 1;  // 转 0-based
    if (start + L > len) L = len - start;
    memcpy(result.data, s.data + start, L);
    result.data[L] = '\0';
    return result;
}

// INSERT: 在位置 P 插入字符串
inline STRING INSERT(const STRING& s, const STRING& ins, DINT P) {
    STRING result;
    DINT lenS = (DINT)strlen(s.data);
    DINT lenI = (DINT)strlen(ins.data);
    if (P < 0) P = 0;
    if (P > lenS) P = lenS;
    DINT total = lenS + lenI;
    if (total > (DINT)STRING_MAX_LEN) total = STRING_MAX_LEN;
    DINT pos = 0;
    // 拷贝 s[0..P)
    for (DINT i = 0; i < P && pos < total; i++) result.data[pos++] = s.data[i];
    // 拷贝 ins
    for (DINT i = 0; i < lenI && pos < total; i++) result.data[pos++] = ins.data[i];
    // 拷贝 s[P..end)
    for (DINT i = P; i < lenS && pos < total; i++) result.data[pos++] = s.data[i];
    result.data[pos] = '\0';
    return result;
}

// DELETE: 从位置 P 删除 L 个字符
inline STRING DELETE(const STRING& s, DINT L, DINT P) {
    STRING result;
    DINT len = (DINT)strlen(s.data);
    if (P < 1) P = 1;
    DINT start = P - 1;
    if (start >= len) return s;
    if (start + L > len) L = len - start;
    DINT pos = 0;
    for (DINT i = 0; i < start; i++) result.data[pos++] = s.data[i];
    for (DINT i = start + L; i < len; i++) result.data[pos++] = s.data[i];
    result.data[pos] = '\0';
    return result;
}

// REPLACE: 从位置 P 替换 L 个字符为 rep
inline STRING REPLACE(const STRING& s, const STRING& rep, DINT L, DINT P) {
    STRING result;
    DINT lenS = (DINT)strlen(s.data);
    DINT lenR = (DINT)strlen(rep.data);
    if (P < 1) P = 1;
    DINT start = P - 1;
    if (start >= lenS) return s;
    if (start + L > lenS) L = lenS - start;
    DINT total = lenS - L + lenR;
    if (total > (DINT)STRING_MAX_LEN) total = STRING_MAX_LEN;
    DINT pos = 0;
    for (DINT i = 0; i < start && pos < total; i++) result.data[pos++] = s.data[i];
    for (DINT i = 0; i < lenR && pos < total; i++) result.data[pos++] = rep.data[i];
    for (DINT i = start + L; i < lenS && pos < total; i++) result.data[pos++] = s.data[i];
    result.data[pos] = '\0';
    return result;
}

// FIND: 查找子串位置（1-based，未找到返回 0）
inline DINT FIND(const STRING& s, const STRING& pattern) {
    const char* found = strstr(s.data, pattern.data);
    if (!found) return 0;
    return (DINT)(found - s.data) + 1;
}

// 向后兼容别名（旧版 API 名称）
inline STRING str_concat(const STRING& a, const STRING& b) { return CONCAT(a, b); }
inline DINT   str_len(const STRING& s) { return LEN(s); }
inline BOOL   str_eq(const STRING& a, const STRING& b) { return STR_EQ(a, b); }


// ─── REF / POINTER ───

template<typename T>
inline T& plc_deref(T* ptr, const char* varName = "") {
    if (!ptr) {
        fprintf(stderr, "NULL pointer dereference: %s\n", varName);
        // thread_local 避免多线程数据竞争
        thread_local T dummy{};
        return dummy;
    }
    return *ptr;
}

template<typename T>
inline BOOL plc_is_valid_ref(T* ptr) {
    return ptr != nullptr ? TRUE : FALSE;
}


// ═══════════════════════════════════════════════════════
// PROGRAM 实例生命周期
// ═══════════════════════════════════════════════════════

// IEC 61131-3 PROGRAM 的启动阶段：
//   INIT     → 变量初始化（仅启动时一次）
//   PRE      → 每次从 STOP→RUN 切换时调用（首扫描前）
//   CYCLIC   → 正常扫描周期执行
//   POST     → 从 RUN→STOP 切换时调用（清理）
//
// 完整的 ProgramInstance 结构和生命周期函数类型定义在 rt_runtime.h 中
// （因为需要 GVL 类型的完整定义）

enum class ProgramPhase : uint8_t {
    UNINIT,     // 未初始化
    INIT,       // 初始化完成，等待首次运行
    FIRST_SCAN, // 首次扫描
    CYCLIC,     // 正常扫描
    STOPPED     // 已停止
};


// ═══════════════════════════════════════════════════════
// 简单单扫描运行时（向后兼容，复杂场景用 rt_runtime.h 的 Scheduler）
// ═══════════════════════════════════════════════════════

struct Runtime {
    ProcessImage image;
    TCI*         tci            = nullptr;
    uint64_t     cycleCount     = 0;
    uint32_t     cycleTimeUs    = 10000;
    bool         running        = false;

    void setCycleTime(uint32_t us) { cycleTimeUs = us; }
    void setTCI(TCI* t) { tci = t; }

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

struct plc_lock {
    std::atomic_flag flag = ATOMIC_FLAG_INIT;

    void lock() {
        while (flag.test_and_set(std::memory_order_acquire)) {
            // spin
        }
    }

    void unlock() {
        flag.clear(std::memory_order_release);
    }

    bool tryLock() {
        return !flag.test_and_set(std::memory_order_acquire);
    }
};

struct plc_lock_guard {
    plc_lock& lock_;
    plc_lock_guard(plc_lock& l) : lock_(l) { lock_.lock(); }
    ~plc_lock_guard() { lock_.unlock(); }
    plc_lock_guard(const plc_lock_guard&) = delete;
    plc_lock_guard& operator=(const plc_lock_guard&) = delete;
};


} // namespace rt_plc

#endif // RT_PLC_H
