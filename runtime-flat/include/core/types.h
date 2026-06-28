/**
 * core/types.h — IEC 61131-3 类型系统 + 类型转换 + 字符串操作
 *
 * 所有 IEC 基本类型、位串类型、时间类型、类型转换函数、
 * 字符串操作、复合类型模板均定义在此。
 * 不含运行时基础设施（ProcessImage、Scheduler 等）。
 */
#pragma once

#include <cstdint>
#include <cstring>
#include <cstdio>
#include <cstdlib>
#include <cmath>
#include <cstdarg>
#include "platform.h"

namespace rt_plc {

// ═══════════════════════════════════════════════════════
// 运行时枚举（系统状态、任务、扫描阶段）
// ═══════════════════════════════════════════════════════

enum class TaskTrigger : uint8_t {
    CYCLIC,
    EVENT,
    FREEWHEELING
};

enum class SystemState : uint8_t {
    STOP,
    STARTING,
    RUN,
    STOPPING,
    ERROR,
    PAUSED
};

enum class TaskState : uint8_t {
    IDLE,
    READY,
    RUNNING,
    OVERRUN,
    ERROR
};

enum class EventEdge : uint8_t {
    RISING,
    FALLING,
    BOTH
};

enum class StartupMode : uint8_t {
    COLD,
    WARM
};

enum class ScanPhase : uint8_t {
    IDLE,
    READ_INPUTS,
    LOGIC_SOLVE,
    WRITE_OUTPUTS,
    HOUSEKEEPING
};

// 时间戳（微秒）
typedef int64_t TIME;

// 错误码
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
    TASK_OVERRUN,
    CONFIG_ERROR,
    SAFETY_VIOLATION,
    RETAIN_CORRUPTED,
    MISC_ERROR,
    USER_ERROR
};

// PROGRAM 生命周期阶段
enum class ProgramPhase : uint8_t {
    UNINIT,
    INIT,
    FIRST_SCAN,
    CYCLIC,
    STOPPED
};


// ═══════════════════════════════════════════════════════
// IEC 61131-3 基本类型
// ═══════════════════════════════════════════════════════

typedef int8_t    SINT;
typedef int16_t   INT;
typedef int32_t   DINT;
typedef int64_t   LINT;

typedef uint8_t   USINT;
typedef uint16_t  UINT;
typedef uint32_t  UDINT;
typedef uint64_t  ULINT;

typedef float     REAL;
typedef double    LREAL;

typedef int8_t    BOOL;
#define TRUE  1
#define FALSE 0


// ═══════════════════════════════════════════════════════
// 位串类型
// ═══════════════════════════════════════════════════════

typedef uint8_t  BYTE;
typedef uint16_t WORD;
typedef uint32_t DWORD;
typedef uint64_t LWORD;


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

inline TIME T_us(int64_t us)  { return us; }
inline TIME T_ms(int64_t ms)  { return ms * 1000; }
inline TIME T_s(int64_t s)    { return s * 1000000; }
inline TIME T_min(int64_t m)  { return m * 60000000; }
inline TIME T_h(int64_t h)    { return h * 3600000000LL; }
inline TIME T_d(int64_t d)    { return d * 86400000000LL; }

inline BOOL T_eq(TIME a, TIME b) { return a == b ? TRUE : FALSE; }
inline BOOL T_ne(TIME a, TIME b) { return a != b ? TRUE : FALSE; }
inline BOOL T_lt(TIME a, TIME b) { return a <  b ? TRUE : FALSE; }
inline BOOL T_le(TIME a, TIME b) { return a <= b ? TRUE : FALSE; }
inline BOOL T_gt(TIME a, TIME b) { return a >  b ? TRUE : FALSE; }
inline BOOL T_ge(TIME a, TIME b) { return a >= b ? TRUE : FALSE; }

typedef int32_t DATE;
typedef int64_t TIME_OF_DAY;
typedef int64_t DATE_AND_TIME;

inline DATE DATE_make(int year, int month, int day) {
    int a = (14 - month) / 12;
    int y = year + 4800 - a;
    int m = month + 12 * a - 3;
    int jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    return (DATE)(jdn - 2440588);
}

inline TIME_OF_DAY TOD_make(int hour, int minute, int second, int ms = 0) {
    return (TIME_OF_DAY)hour * 3600000000LL
         + (TIME_OF_DAY)minute * 60000000LL
         + (TIME_OF_DAY)second * 1000000LL
         + (TIME_OF_DAY)ms * 1000LL;
}

inline DATE_AND_TIME DT_make(int year, int month, int day,
                             int hour, int minute, int second) {
    DATE d = DATE_make(year, month, day);
    return (DATE_AND_TIME)d * 86400000000LL + TOD_make(hour, minute, second);
}

inline DATE DT_to_date(DATE_AND_TIME dt) { return (DATE)(dt / 86400000000LL); }
inline TIME_OF_DAY DT_to_tod(DATE_AND_TIME dt) { return dt % 86400000000LL; }

inline DATE_AND_TIME NOW() {
    return (DATE_AND_TIME)platform::nowUs();
}

inline TIME_OF_DAY NOW_TOD() {
    auto now = NOW();
    return now % 86400000000LL;
}

inline DATE NOW_DATE() {
    auto now = NOW();
    return (DATE)(now / 86400000000LL);
}


// ═══════════════════════════════════════════════════════
// 位操作
// ═══════════════════════════════════════════════════════

template<typename T> inline T plc_and(T a, T b)  { return a & b; }
template<typename T> inline T plc_or(T a, T b)   { return a | b; }
template<typename T> inline T plc_xor(T a, T b)  { return a ^ b; }
template<typename T> inline T plc_not(T a)        { return (T)~a; }

template<typename T> inline T SHL(T val, INT n) { return (T)(val << n); }
template<typename T> inline T SHR(T val, INT n) { return (T)((unsigned long long)val >> n); }

inline BYTE ROL(BYTE val, INT n) {
    n = n & 7;
    return (BYTE)((val << n) | (val >> (8 - n)));
}
inline BYTE ROR(BYTE val, INT n) {
    n = n & 7;
    return (BYTE)((val >> n) | (val << (8 - n)));
}


// ═══════════════════════════════════════════════════════
// 类型转换
// ═══════════════════════════════════════════════════════

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

// → BYTE
inline BYTE  TO_BYTE(INT v)     { return (BYTE)v; }
inline BYTE  TO_BYTE(DINT v)    { return (BYTE)v; }
inline BYTE  TO_BYTE(LINT v)    { return (BYTE)v; }
inline BYTE  TO_BYTE(UDINT v)   { return (BYTE)v; }
inline BYTE  TO_BYTE(USINT v)   { return (BYTE)v; }
inline BYTE  TO_BYTE(UINT v)    { return (BYTE)v; }
inline BYTE  TO_BYTE(REAL v)    { return (BYTE)v; }
inline BYTE  TO_BYTE(LREAL v)   { return (BYTE)v; }
inline BYTE  TO_BYTE(BOOL v)    { return (BYTE)v; }

// → WORD
inline WORD  TO_WORD(INT v)     { return (WORD)v; }
inline WORD  TO_WORD(DINT v)    { return (WORD)v; }
inline WORD  TO_WORD(UDINT v)   { return (WORD)v; }
inline WORD  TO_WORD(UINT v)    { return (WORD)v; }
inline WORD  TO_WORD(BYTE v)    { return (WORD)v; }
inline WORD  TO_WORD(REAL v)    { return (WORD)v; }
inline WORD  TO_WORD(LREAL v)   { return (WORD)v; }

// → DWORD
inline DWORD TO_DWORD(INT v)    { return (DWORD)v; }
inline DWORD TO_DWORD(DINT v)   { return (DWORD)v; }
inline DWORD TO_DWORD(UDINT v)  { return (DWORD)v; }
inline DWORD TO_DWORD(UINT v)   { return (DWORD)v; }
inline DWORD TO_DWORD(BYTE v)   { return (DWORD)v; }
inline DWORD TO_DWORD(REAL v)   { return (DWORD)v; }
inline DWORD TO_DWORD(LREAL v)  { return (DWORD)v; }

// → TIME
inline TIME  TO_TIME(INT v)     { return (TIME)v; }
inline TIME  TO_TIME(DINT v)    { return (TIME)v; }
inline TIME  TO_TIME(UDINT v)   { return (TIME)v; }
inline TIME  TO_TIME(REAL v)    { return (TIME)v; }

// → UINT / UDINT（补充）
inline UINT  TO_UINT(UDINT v)   { return (UINT)v; }
inline UINT  TO_UINT(WORD v)    { return (UINT)v; }
inline UINT  TO_UINT(BYTE v)    { return (UINT)v; }
inline UDINT TO_UDINT(UINT v)   { return (UDINT)v; }
inline UDINT TO_UDINT(BYTE v)   { return (UDINT)v; }
inline UDINT TO_UDINT(INT v)    { return (UDINT)v; }
inline UDINT TO_UDINT(REAL v)   { return (UDINT)v; }
inline USINT TO_USINT(BYTE v)   { return (USINT)v; }

// ─── IEC 61131-3 X_TO_Y 类型转换 ───

inline DINT  UDINT_TO_DINT(UDINT v)  { return (DINT)v; }
inline INT   UDINT_TO_INT(UDINT v)   { return (INT)v; }
inline DWORD UDINT_TO_DWORD(UDINT v) { return (DWORD)v; }
inline WORD  UDINT_TO_WORD(UDINT v)  { return (WORD)v; }
inline BYTE  UDINT_TO_BYTE(UDINT v)  { return (BYTE)v; }
inline STRING UDINT_TO_STRING(UDINT v) { return TO_STRING((DINT)v); }
inline UINT  UDINT_TO_UINT(UDINT v)  { return (UINT)v; }
inline REAL  UDINT_TO_REAL(UDINT v)  { return (REAL)v; }
inline TIME  UDINT_TO_TIME(UDINT v)  { return (TIME)v; }

inline TIME   DINT_TO_TIME(DINT v)   { return (TIME)v; }
inline UDINT  DINT_TO_UDINT(DINT v)  { return (UDINT)v; }
inline INT    DINT_TO_INT(DINT v)    { return (INT)v; }
inline REAL   DINT_TO_REAL(DINT v)   { return (REAL)v; }
inline DWORD  DINT_TO_DWORD(DINT v)  { return (DWORD)v; }
inline WORD   DINT_TO_WORD(DINT v)   { return (WORD)v; }
inline STRING DINT_TO_STRING(DINT v) { return TO_STRING(v); }

inline UDINT  INT_TO_UDINT(INT v)    { return (UDINT)v; }
inline DINT   INT_TO_DINT(INT v)     { return (DINT)v; }
inline REAL   INT_TO_REAL(INT v)     { return (REAL)v; }
inline DWORD  INT_TO_DWORD(INT v)    { return (DWORD)v; }
inline BYTE   INT_TO_BYTE(INT v)     { return (BYTE)v; }
inline WORD   INT_TO_WORD(INT v)     { return (WORD)v; }
inline STRING INT_TO_STRING(INT v)   { return TO_STRING(v); }
inline TIME   INT_TO_TIME(INT v)     { return (TIME)v; }
inline UINT   INT_TO_UINT(INT v)     { return (UINT)v; }

inline DINT   REAL_TO_DINT(REAL v)   { return (DINT)v; }
inline UDINT  REAL_TO_UDINT(REAL v)  { return (UDINT)v; }
inline DWORD  REAL_TO_DWORD(REAL v)  { DWORD r; memcpy(&r, &v, sizeof(r)); return r; }
inline INT    REAL_TO_INT(REAL v)    { return (INT)v; }
inline WORD   REAL_TO_WORD(REAL v)   { return (WORD)v; }
inline BYTE   REAL_TO_BYTE(REAL v)   { return (BYTE)v; }
inline TIME   REAL_TO_TIME(REAL v)   { return (TIME)v; }
inline STRING REAL_TO_STRING(REAL v) { return TO_STRING(v); }

inline DINT   DWORD_TO_DINT(DWORD v)   { return (DINT)v; }
inline INT    DWORD_TO_INT(DWORD v)    { return (INT)v; }
inline WORD   DWORD_TO_WORD(DWORD v)   { return (WORD)v; }
inline BYTE   DWORD_TO_BYTE(DWORD v)   { return (BYTE)v; }
inline TIME   DWORD_TO_TIME(DWORD v)   { return (TIME)v; }
inline UDINT  DWORD_TO_UDINT(DWORD v)  { return (UDINT)v; }
inline STRING DWORD_TO_STRING(DWORD v) { return TO_STRING((DINT)v); }

inline DINT   WORD_TO_DINT(WORD v)    { return (DINT)v; }
inline INT    WORD_TO_INT(WORD v)     { return (INT)v; }
inline DWORD  WORD_TO_DWORD(WORD v)   { return (DWORD)v; }
inline BYTE   WORD_TO_BYTE(WORD v)    { return (BYTE)v; }
inline UDINT  WORD_TO_UDINT(WORD v)   { return (UDINT)v; }
inline UINT   WORD_TO_UINT(WORD v)    { return (UINT)v; }
inline STRING WORD_TO_STRING(WORD v)  { char buf[32]; snprintf(buf, sizeof(buf), "%u", (unsigned)v); return STRING(buf); }

inline DINT   BYTE_TO_DINT(BYTE v)    { return (DINT)v; }
inline INT    BYTE_TO_INT(BYTE v)     { return (INT)v; }
inline DWORD  BYTE_TO_DWORD(BYTE v)   { return (DWORD)v; }
inline WORD   BYTE_TO_WORD(BYTE v)    { return (WORD)v; }
inline UDINT  BYTE_TO_UDINT(BYTE v)   { return (UDINT)v; }
inline UINT   BYTE_TO_UINT(BYTE v)    { return (UINT)v; }
inline USINT  BYTE_TO_USINT(BYTE v)   { return (USINT)v; }
inline STRING BYTE_TO_STRING(BYTE v)  { char buf[32]; snprintf(buf, sizeof(buf), "%u", (unsigned)v); return STRING(buf); }

inline DINT   UINT_TO_DINT(UINT v)    { return (DINT)v; }
inline INT    UINT_TO_INT(UINT v)     { return (INT)v; }
inline DWORD  UINT_TO_DWORD(UINT v)   { return (DWORD)v; }
inline BYTE   UINT_TO_BYTE(UINT v)    { return (BYTE)v; }
inline WORD   UINT_TO_WORD(UINT v)    { return (WORD)v; }
inline UDINT  UINT_TO_UDINT(UINT v)   { return (UDINT)v; }
inline REAL   UINT_TO_REAL(UINT v)    { return (REAL)v; }

inline INT    USINT_TO_INT(USINT v)   { return (INT)v; }
inline BYTE   USINT_TO_BYTE(USINT v)  { return (BYTE)v; }
inline REAL   USINT_TO_REAL(USINT v)  { return (REAL)v; }
inline DINT   USINT_TO_DINT(USINT v)  { return (DINT)v; }

inline INT    SINT_TO_INT(SINT v)     { return (INT)v; }
inline DINT   SINT_TO_DINT(SINT v)    { return (DINT)v; }

inline INT    STRING_TO_INT(STRING s)    { return (INT)atoi(s.data); }
inline REAL   STRING_TO_REAL(STRING s)   { return (REAL)atof(s.data); }
inline DWORD  STRING_TO_DWORD(STRING s)  { return (DWORD)strtoul(s.data, nullptr, 10); }
inline WORD   STRING_TO_WORD(STRING s)   { return (WORD)strtoul(s.data, nullptr, 10); }
inline UDINT  STRING_TO_UDINT(STRING s)  { return (UDINT)strtoul(s.data, nullptr, 10); }
inline TIME   STRING_TO_TIME(STRING s)   { return (TIME)strtoll(s.data, nullptr, 10); }

inline BYTE   BOOL_TO_BYTE(BOOL v)    { return (BYTE)v; }
inline INT    BOOL_TO_INT(BOOL v)     { return (INT)v; }
inline DWORD  BOOL_TO_DWORD(BOOL v)   { return (DWORD)v; }
inline UDINT  BOOL_TO_UDINT(BOOL v)   { return (UDINT)v; }
inline STRING BOOL_TO_STRING(BOOL v)  { return TO_STRING(v); }

inline DINT   TIME_TO_DINT(TIME v)    { return (DINT)v; }
inline INT    TIME_TO_INT(TIME v)     { return (INT)v; }
inline DWORD  TIME_TO_DWORD(TIME v)   { return (DWORD)v; }
inline REAL   TIME_TO_REAL(TIME v)    { return (REAL)v; }
inline STRING TIME_TO_TOD(TIME v) {
    int64_t us = v % 86400000000LL;
    if (us < 0) us += 86400000000LL;
    int h = (int)(us / 3600000000LL);
    int m = (int)((us % 3600000000LL) / 60000000LL);
    int s = (int)((us % 60000000LL) / 1000000LL);
    int ms = (int)((us % 1000000LL) / 1000LL);
    char buf[32];
    snprintf(buf, sizeof(buf), "%02d:%02d:%02d.%03d", h, m, s, ms);
    return STRING(buf);
}

inline TIME   TOD_TO_TIME(STRING s) {
    int h = 0, m = 0, sec = 0, ms = 0;
    sscanf(s.data, "%d:%d:%d.%d", &h, &m, &sec, &ms);
    return (TIME)h * 3600000000LL + (TIME)m * 60000000LL
         + (TIME)sec * 1000000LL + (TIME)ms * 1000LL;
}
inline REAL   TOD_TO_REAL2(STRING s) { return (REAL)TOD_TO_TIME(s); }

inline UDINT  DATE_TO_UDINT(DATE v)   { return (UDINT)v; }
inline DATE   DATE_TO_DT(DATE v)      { return v; }
inline STRING DATE_TO_STRING2(DATE v) {
    int64_t jdn = (int64_t)v + 2440588;
    int a = (int)((14 - (jdn + 32044)) / 46061);
    int y = (int)(jdn + 32044 - a * 46061);
    int m = (int)((153 * ((y + 256) / 1095) + 2) / 5);
    int d = (int)(y - m * 365 / 10);
    int month = m - 3 + 1;
    if (month <= 0) { month += 12; y--; }
    m += 3;
    y = (int)(jdn + 32044 + (m - 3) * 10);
    char buf[32];
    snprintf(buf, sizeof(buf), "%d-%02d-%02d", y, month, d);
    return STRING(buf);
}
inline REAL   DATE_TO_REAL2(DATE v)   { return (REAL)v; }
inline DWORD  DATE_TO_DWORD2(DATE v)  { return (DWORD)(uint32_t)v; }

inline DATE   DT_TO_DATE(DATE_AND_TIME v)  { return DT_to_date(v); }
inline DATE   DT_TO_DATE2(DATE_AND_TIME v) { return DT_to_date(v); }
inline STRING DT_TO_TOD2(DATE_AND_TIME v)  { return TIME_TO_TOD((TIME)DT_to_tod(v)); }
inline DATE   DT_TO_SDT(DATE_AND_TIME v)   { return DT_to_date(v); }
inline STRING DT_TO_STRF(DATE_AND_TIME v) {
    DATE d = DT_to_date(v);
    return DATE_TO_STRING2(d);
}

inline DATE   SDT_TO_DATE(DATE v)   { return v; }
inline DATE   SDT_TO_DT(DATE v)     { return v; }
inline STRING SDT_TO_TOD(DATE v)    { return TIME_TO_TOD((TIME)v); }


// ─── 格式化字符串转换 ───

inline STRING REAL_TO_STRF(REAL v) {
    STRING s;
    snprintf(s.data, STRING_MAX_LEN + 1, "%f", (double)v);
    return s;
}

inline STRING DWORD_TO_STRB(DWORD v) {
    char buf[33];
    for (int i = 0; i < 32; i++) buf[i] = (v & (1u << (31 - i))) ? '1' : '0';
    buf[32] = '\0';
    return STRING(buf);
}

inline STRING DWORD_TO_STRH(DWORD v) {
    char buf[16];
    snprintf(buf, sizeof(buf), "%X", v);
    return STRING(buf);
}

inline STRING DWORD_TO_STRF(DWORD v) {
    char buf[16];
    snprintf(buf, sizeof(buf), "%u", v);
    return STRING(buf);
}

inline STRING BYTE_TO_STRB(BYTE v) {
    char buf[9];
    for (int i = 0; i < 8; i++) buf[i] = (v & (1 << (7 - i))) ? '1' : '0';
    buf[8] = '\0';
    return STRING(buf);
}

inline STRING BYTE_TO_STRH(BYTE v) {
    char buf[8];
    snprintf(buf, sizeof(buf), "%X", (unsigned)v);
    return STRING(buf);
}

inline STRING BYTE_TO_BITS(BYTE v) { return BYTE_TO_STRB(v); }

inline BYTE BYTE_TO_TIME2(BYTE v) { return v; }

inline BYTE BYTE_TO_GRAY(BYTE v) { return v ^ (v >> 1); }
inline BYTE GRAY_TO_BYTE(BYTE v) {
    BYTE mask = v >> 1;
    while (mask) { v ^= mask; mask >>= 1; }
    return v;
}

inline INT  BCD_TO_INT(BYTE v) { return (INT)(((v >> 4) & 0xF) * 10 + (v & 0xF)); }
inline BYTE INT_TO_BCD(INT v) {
    return (BYTE)((((v / 10) % 10) << 4) | (v % 10));
}

inline STRING UINT_TO_STRING(UINT v) {
    char buf[32];
    snprintf(buf, sizeof(buf), "%u", (unsigned)v);
    return STRING(buf);
}

inline STRING STRING_TO_URL(STRING s) { return s; }
inline STRING STRING_TO_BUFFER(STRING s) { return s; }


// ─── MOVE / TRUNC / FLOOR / EXP ───

template<typename T> inline T MOVE(T v) { return v; }

inline INT TRUNC(REAL v) { return (INT)v; }

inline INT FLOOR(REAL v) { return (INT)std::floor((double)v); }

inline REAL EXP(REAL x) { return (REAL)std::exp((double)x); }


// ═══════════════════════════════════════════════════════
// 数学函数
// ═══════════════════════════════════════════════════════

inline SINT  ABS(SINT x)   { return x < 0 ? -x : x; }
inline INT   ABS(INT x)    { return x < 0 ? -x : x; }
inline DINT  ABS(DINT x)   { return x < 0 ? -x : x; }
inline LINT  ABS(LINT x)   { return x < 0 ? -x : x; }
inline REAL  ABS(REAL x)   { return x < 0 ? -x : x; }
inline LREAL ABS(LREAL x)  { return x < 0 ? -x : x; }

inline REAL  SQRT(REAL x)  { return (REAL)std::sqrt((double)x); }
inline LREAL SQRT(LREAL x) { return std::sqrt(x); }

inline REAL  EXPT(REAL base, DINT exp) {
    REAL result = 1.0f;
    bool neg = exp < 0;
    if (neg) exp = -exp;
    for (DINT i = 0; i < exp; i++) result *= base;
    return neg ? 1.0f / result : result;
}

inline REAL  LN(REAL x)   { return (REAL)std::log((double)x); }
inline LREAL LN(LREAL x)  { return std::log(x); }
inline REAL  LOG(REAL x)  { return (REAL)std::log10((double)x); }
inline LREAL LOG(LREAL x) { return std::log10(x); }

template<typename T> inline T MIN(T a, T b) { return a < b ? a : b; }
template<typename T> inline T MAX(T a, T b) { return a > b ? a : b; }

inline SINT  MOD(SINT a, SINT b)   { return b ? a % b : 0; }
inline INT   MOD(INT a, INT b)     { return b ? a % b : 0; }
inline DINT  MOD(DINT a, DINT b)   { return b ? a % b : 0; }
inline LINT  MOD(LINT a, LINT b)   { return b ? a % b : 0; }

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

struct TOF {
    BOOL  input   = TRUE;
    TIME  preset  = 0;
    TIME  elapsed = 0;
    BOOL  output  = FALSE;

    void update(BOOL IN, TIME PT, TIME cycleTimeUs) {
        preset = PT;
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
            RT_LOG_ERR("ARRAY out of bounds: index %zu, size %zu\n", index, N);
            index = N - 1;
        }
        return elements[index];
    }

    const T& at(size_t index) const {
        if (index >= N) {
            RT_LOG_ERR("ARRAY out of bounds: index %zu, size %zu\n", index, N);
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

#define PLC_ENUM_COUNT(enumType) (static_cast<int>(enumType::_COUNT))


// ═══════════════════════════════════════════════════════
// 字符串操作
// ═══════════════════════════════════════════════════════

inline void str_assign(STRING& dst, const char* src) {
    strncpy(dst.data, src, STRING_MAX_LEN);
    dst.data[STRING_MAX_LEN] = '\0';
}

inline void str_assign(STRING& dst, const STRING& src) {
    memcpy(dst.data, src.data, STRING_MAX_LEN + 1);
}

inline STRING CONCAT(const STRING& a, const STRING& b) {
    STRING result;
    size_t lenA = strlen(a.data);
    size_t remain = STRING_MAX_LEN - lenA;
    memcpy(result.data, a.data, lenA);
    strncpy(result.data + lenA, b.data, remain);
    result.data[STRING_MAX_LEN] = '\0';
    return result;
}

inline DINT LEN(const STRING& s) {
    return (DINT)strlen(s.data);
}

inline BOOL STR_EQ(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) == 0 ? TRUE : FALSE; }
inline BOOL STR_NE(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) != 0 ? TRUE : FALSE; }
inline BOOL STR_LT(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) <  0 ? TRUE : FALSE; }
inline BOOL STR_LE(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) <= 0 ? TRUE : FALSE; }
inline BOOL STR_GT(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) >  0 ? TRUE : FALSE; }
inline BOOL STR_GE(const STRING& a, const STRING& b) { return strcmp(a.data, b.data) >= 0 ? TRUE : FALSE; }

inline STRING LEFT(const STRING& s, DINT L) {
    STRING result;
    DINT len = (DINT)strlen(s.data);
    if (L > len) L = len;
    if (L < 0) L = 0;
    memcpy(result.data, s.data, L);
    result.data[L] = '\0';
    return result;
}

inline STRING RIGHT(const STRING& s, DINT L) {
    STRING result;
    DINT len = (DINT)strlen(s.data);
    if (L > len) L = len;
    if (L < 0) L = 0;
    memcpy(result.data, s.data + (len - L), L);
    result.data[L] = '\0';
    return result;
}

inline STRING MID(const STRING& s, DINT L, DINT P) {
    STRING result;
    DINT len = (DINT)strlen(s.data);
    if (P < 1) P = 1;
    if (P > len) { result.data[0] = '\0'; return result; }
    DINT start = P - 1;
    if (start + L > len) L = len - start;
    memcpy(result.data, s.data + start, L);
    result.data[L] = '\0';
    return result;
}

inline STRING INSERT(const STRING& s, const STRING& ins, DINT P) {
    STRING result;
    DINT lenS = (DINT)strlen(s.data);
    DINT lenI = (DINT)strlen(ins.data);
    if (P < 0) P = 0;
    if (P > lenS) P = lenS;
    DINT total = lenS + lenI;
    if (total > (DINT)STRING_MAX_LEN) total = STRING_MAX_LEN;
    DINT pos = 0;
    for (DINT i = 0; i < P && pos < total; i++) result.data[pos++] = s.data[i];
    for (DINT i = 0; i < lenI && pos < total; i++) result.data[pos++] = ins.data[i];
    for (DINT i = P; i < lenS && pos < total; i++) result.data[pos++] = s.data[i];
    result.data[pos] = '\0';
    return result;
}

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

inline DINT FIND(const STRING& s, const STRING& pattern) {
    const char* found = strstr(s.data, pattern.data);
    if (!found) return 0;
    return (DINT)(found - s.data) + 1;
}

inline STRING str_concat(const STRING& a, const STRING& b) { return CONCAT(a, b); }
inline DINT   str_len(const STRING& s) { return LEN(s); }
inline BOOL   str_eq(const STRING& a, const STRING& b) { return STR_EQ(a, b); }


// ═══════════════════════════════════════════════════════
// REF / POINTER
// ═══════════════════════════════════════════════════════

template<typename T>
inline T& plc_deref(T* ptr, const char* varName = "") {
    if (!ptr) {
        RT_LOG_ERR("NULL pointer dereference: %s\n", varName);
        RT_THREAD_LOCAL T dummy{};
        return dummy;
    }
    return *ptr;
}

template<typename T>
inline BOOL plc_is_valid_ref(T* ptr) {
    return ptr != nullptr ? TRUE : FALSE;
}

} // namespace rt_plc
