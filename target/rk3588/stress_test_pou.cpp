/**
 * stress_test_pou.cpp — PLC 周期压力测试 POU
 *
 * 模拟真实工业控制场景的复合负载：
 *   - 4 个级联 PID 回路（浮点密集）
 *   - 64 阶 FIR 滤波器（数组/乘加密集）
 *   - 4x4 矩阵乘法（控制坐标变换）
 *   - 8 状态复杂状态机（逻辑分支密集）
 *   - 32 位打包/解包（位操作密集）
 *   - IEC 定时器/计数器链（功能块密集）
 *   - ProcessImage I/O 读写
 *
 * 负载级别通过 STRESS_LOAD 宏控制（1-10，默认 5）
 *
 * 编译时加入 stress_test_pou.cpp 并注册到任务即可。
 */

#include "rt_runtime.h"
#include "rt_plc.h"

#include <cstring>
#include <cmath>

using namespace rt_plc;

// ─── 负载级别（1=最轻，10=最重） ───
#ifndef STRESS_LOAD
#define STRESS_LOAD 5
#endif

// ─── 负载缩放系数 ───
#define STRESS_PID_LOOPS       ((STRESS_LOAD) * 1)       // 1-10 个 PID
#define STRESS_FIR_TAPS        ((STRESS_LOAD) * 6 + 4)   // 10-64 阶
#define STRESS_MATRIX_SIZE     ((STRESS_LOAD) > 7 ? 8 : 4)
#define STRESS_STATE_COUNT     ((STRESS_LOAD) > 5 ? 12 : 8)
#define STRESS_BIT_CHANNELS    ((STRESS_LOAD) * 4)        // 4-40
#define STRESS_ARRAY_SIZE      ((STRESS_LOAD) * 10 + 10)  // 20-110


// ═══════════════════════════════════════════════════════
// 1. PID 控制器
// ═══════════════════════════════════════════════════════

struct PIDController {
    REAL Kp, Ki, Kd;
    REAL setpoint = 0.0f;
    REAL integral = 0.0f;
    REAL prevError = 0.0f;
    REAL output = 0.0f;
    REAL outputMin = -100.0f;
    REAL outputMax = 100.0f;
    REAL dt = 0.001f;

    void reset() {
        integral = 0.0f;
        prevError = 0.0f;
        output = 0.0f;
    }

    REAL compute(REAL feedback) {
        REAL error = setpoint - feedback;
        integral += error * dt;
        if (integral > outputMax) integral = outputMax;
        if (integral < outputMin) integral = outputMin;
        REAL derivative = (error - prevError) / dt;
        REAL out = Kp * error + Ki * integral + Kd * derivative;
        if (out > outputMax) out = outputMax;
        if (out < outputMin) out = outputMin;
        output = out;
        prevError = error;
        return out;
    }
};


// ═══════════════════════════════════════════════════════
// 2. FIR 滤波器
// ═══════════════════════════════════════════════════════

struct FIRFilter {
    REAL buffer[64];
    REAL coeffs[64];
    int index = 0;
    int taps;

    FIRFilter(int tapCount) : taps(tapCount > 64 ? 64 : tapCount) {
        for (int i = 0; i < taps; i++) {
            buffer[i] = 0.0f;
            coeffs[i] = 1.0f / (REAL)taps;
        }
    }

    REAL process(REAL input) {
        buffer[index] = input;
        index = (index + 1) % taps;
        REAL sum = 0.0f;
        for (int i = 0; i < taps; i++) {
            int idx = (index - 1 - i + taps) % taps;
            sum += buffer[idx] * coeffs[i];
        }
        return sum;
    }
};


// ═══════════════════════════════════════════════════════
// 3. 4x4 矩阵（齐次坐标变换）
// ═══════════════════════════════════════════════════════

struct Matrix4x4 {
    REAL m[4][4];

    Matrix4x4() {
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                m[i][j] = (i == j) ? 1.0f : 0.0f;
    }

    Matrix4x4 multiply(const Matrix4x4& other) const {
        Matrix4x4 result;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                REAL sum = 0.0f;
                for (int k = 0; k < 4; k++) {
                    sum += m[i][k] * other.m[k][j];
                }
                result.m[i][j] = sum;
            }
        }
        return result;
    }

    void rotateX(REAL angle) {
        REAL c = cos(angle);
        REAL s = sin(angle);
        Matrix4x4 r;
        r.m[1][1] = c;  r.m[1][2] = -s;
        r.m[2][1] = s;  r.m[2][2] = c;
        *this = multiply(r);
    }

    void rotateY(REAL angle) {
        REAL c = cos(angle);
        REAL s = sin(angle);
        Matrix4x4 r;
        r.m[0][0] = c;  r.m[0][2] = s;
        r.m[2][0] = -s; r.m[2][2] = c;
        *this = multiply(r);
    }
};


// ═══════════════════════════════════════════════════════
// 4. 复杂状态机
// ═══════════════════════════════════════════════════════

struct ComplexStateMachine {
    enum State : uint8_t {
        IDLE,
        INIT,
        HOMING,
        APPROACH,
        WAIT,
        PROCESS,
        VERIFY,
        COMPLETE,
        ERROR_RECOVERY,
        EMERGENCY_STOP,
        CALIBRATE,
        SHUTDOWN
    };

    State currentState = IDLE;
    State lastState = IDLE;
    int timer = 0;
    int cycleCount = 0;
    REAL position = 0.0f;
    REAL sensorValue = 0.0f;

    State transition(REAL sensor, BOOL estop, BOOL startSignal) {
        cycleCount++;
        lastState = currentState;
        sensorValue = sensor;

        if (estop) {
            currentState = EMERGENCY_STOP;
            return currentState;
        }

        switch (currentState) {
            case IDLE:
                if (startSignal) currentState = INIT;
                break;
            case INIT:
                timer++;
                if (timer > 5) { timer = 0; currentState = HOMING; }
                break;
            case HOMING:
                position += 0.1f;
                if (position >= 10.0f) {
                    position = 0.0f;
                    currentState = APPROACH;
                }
                break;
            case APPROACH:
                position += 0.5f;
                if (sensor > 50.0f) currentState = WAIT;
                break;
            case WAIT:
                timer++;
                if (timer > 10 && sensor < 30.0f) {
                    timer = 0;
                    currentState = PROCESS;
                }
                break;
            case PROCESS:
                timer++;
                position = 100.0f * sin((REAL)timer * 0.1f);
                if (timer > 20) { timer = 0; currentState = VERIFY; }
                break;
            case VERIFY:
                if (fabs(position) < 1.0f) currentState = COMPLETE;
                else currentState = ERROR_RECOVERY;
                break;
            case COMPLETE:
                if (cycleCount % 100 == 0) currentState = IDLE;
                break;
            case ERROR_RECOVERY:
                timer++;
                if (timer > 5) { timer = 0; currentState = IDLE; }
                break;
            case EMERGENCY_STOP:
                position = 0.0f;
                if (!estop && timer++ > 20) {
                    timer = 0;
                    currentState = IDLE;
                }
                break;
            case CALIBRATE:
                timer++;
                if (timer > 30) { timer = 0; currentState = IDLE; }
                break;
            case SHUTDOWN:
                break;
        }
        return currentState;
    }
};


// ═══════════════════════════════════════════════════════
// 5. 压力测试 POU 状态
// ═══════════════════════════════════════════════════════

struct StressPOUState {
    // PID 级联 (最多 10 个)
    PIDController pidLoops[10];
    int pidCount;

    // FIR 滤波器
    FIRFilter* firFilter;

    // 矩阵变换
    Matrix4x4 transform;
    Matrix4x4 accumulated;

    // 状态机
    ComplexStateMachine stateMachine;

    // 位打包
    DWORD packedBits = 0;
    BOOL bitChannels[40];

    // IEC 定时器/计数器
    TON timerOn[8];
    TOF timerOff[4];
    CTU counterUp[4];
    CTUD counterUpDown[2];

    // 大数组操作
    REAL arrayA[110];
    REAL arrayB[110];
    REAL arrayC[110];

    // I/O 模拟
    REAL sensorAccum = 0.0f;
    DWORD ioCycle = 0;

    // 运行计数
    uint64_t cycleIndex = 0;

    StressPOUState() {
        pidCount = STRESS_PID_LOOPS > 10 ? 10 : STRESS_PID_LOOPS;
        for (int i = 0; i < pidCount; i++) {
            pidLoops[i].Kp = 2.0f + (REAL)i * 0.5f;
            pidLoops[i].Ki = 0.1f + (REAL)i * 0.05f;
            pidLoops[i].Kd = 0.05f + (REAL)i * 0.02f;
            pidLoops[i].setpoint = (REAL)(i * 10);
        }

        firFilter = new FIRFilter(STRESS_FIR_TAPS);

        for (int i = 0; i < 40; i++) bitChannels[i] = FALSE;
        for (int i = 0; i < 8; i++) timerOn[i].PT = T_ms(5 + i * 2);
        for (int i = 0; i < 4; i++) timerOff[i].PT = T_ms(3 + i * 3);
        for (int i = 0; i < 4; i++) counterUp[i].PV = 10 + i * 5;
        counterUpDown[0].PV = 100; counterUpDown[1].PV = 200;

        for (int i = 0; i < STRESS_ARRAY_SIZE; i++) {
            arrayA[i] = (REAL)(i * 0.1f);
            arrayB[i] = (REAL)((STRESS_ARRAY_SIZE - i) * 0.1f);
            arrayC[i] = 0.0f;
        }
    }

    ~StressPOUState() {
        delete firFilter;
    }
};

// 全局 POU 状态（PLC 生命周期内持久）
static StressPOUState* g_state = nullptr;


// ═══════════════════════════════════════════════════════
// Init — 初始化所有状态
// ═══════════════════════════════════════════════════════

static void stressInit(GVL& gvl, ProcessImage& io) {
    if (g_state) delete g_state;
    g_state = new StressPOUState();
    platform::logInfo("[STRESS] POU init complete: load=%d, pid=%d, fir=%d, matrix=%dx%d, state=%d, bits=%d, array=%d\n",
        STRESS_LOAD,
        g_state->pidCount,
        STRESS_FIR_TAPS,
        STRESS_MATRIX_SIZE, STRESS_MATRIX_SIZE,
        STRESS_STATE_COUNT,
        STRESS_BIT_CHANNELS,
        STRESS_ARRAY_SIZE);
}


// ═══════════════════════════════════════════════════════
// Cyclic — 每周期执行的压力负载
// ═══════════════════════════════════════════════════════

static void stressCyclic(GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
    if (!g_state) return;

    StressPOUState& s = *g_state;
    s.cycleIndex++;

    REAL dt = (REAL)cycleTimeUs / 1000000.0f;

    // ── 1. 级联 PID 计算（CPU 浮点密集） ──
    REAL feedback = 0.0f;
    for (int i = 0; i < s.pidCount; i++) {
        s.pidLoops[i].dt = dt;
        if (i == 0) {
            feedback = 30.0f + 10.0f * sin((REAL)s.cycleIndex * 0.01f);
        }
        REAL out = s.pidLoops[i].compute(feedback);
        feedback = out;  // 级联：前一级输出作为后一级反馈
    }

    // ── 2. FIR 滤波器（数组/乘加密集） ──
    REAL rawSignal = 50.0f + 25.0f * sin((REAL)s.cycleIndex * 0.005f)
                    + 10.0f * sin((REAL)s.cycleIndex * 0.1f)
                    + 5.0f * ((REAL)(s.cycleIndex % 7) - 3.0f);
    REAL filtered = s.firFilter->process(rawSignal);
    (void)filtered;

    // ── 3. 矩阵变换（4x4 浮点密集） ──
    s.transform.rotateX(dt * 0.5f);
    s.transform.rotateY(dt * 0.3f);
    s.accumulated = s.accumulated.multiply(s.transform);

    // ── 4. 复杂状态机（逻辑分支密集） ──
    BOOL estop = (s.cycleIndex % 5000 == 4999) ? TRUE : FALSE;
    BOOL startSignal = (s.cycleIndex == 1) ? TRUE : FALSE;
    REAL sensor = filtered + feedback;
    s.stateMachine.transition(sensor, estop, startSignal);

    // ── 5. 位打包/解包（位操作密集） ──
    for (int i = 0; i < STRESS_BIT_CHANNELS && i < 40; i++) {
        s.bitChannels[i] = (s.cycleIndex + i) % 3 != 0;
    }
    s.packedBits = 0;
    for (int i = 0; i < STRESS_BIT_CHANNELS && i < 32; i++) {
        if (s.bitChannels[i]) s.packedBits |= (1u << i);
    }
    DWORD unpacked = s.packedBits;
    for (int i = 0; i < STRESS_BIT_CHANNELS && i < 32; i++) {
        s.bitChannels[i] = (unpacked & (1u << i)) != 0;
    }

    // ── 6. IEC 定时器/计数器链（功能块密集） ──
    TIME dtTime = cycleTimeUs;
    for (int i = 0; i < 8; i++) {
        s.timerOn[i].IN = (s.cycleIndex + i) % 50 < 25;
        s.timerOn[i].update(dtTime);
    }
    for (int i = 0; i < 4; i++) {
        s.timerOff[i].IN = (s.cycleIndex + i) % 40 < 20;
        s.timerOff[i].update(dtTime);
    }
    for (int i = 0; i < 4; i++) {
        s.counterUp[i].CU = (s.cycleIndex + i * 3) % 5 == 0;
        s.counterUp[i].R = (s.cycleIndex % 200 == 0);
        s.counterUp[i].update(dtTime);
    }
    s.counterUpDown[0].CU = s.cycleIndex % 2 == 0;
    s.counterUpDown[0].CD = s.cycleIndex % 3 == 0;
    s.counterUpDown[0].update(dtTime);

    // ── 7. 大数组操作（内存带宽密集） ──
    for (int i = 0; i < STRESS_ARRAY_SIZE; i++) {
        s.arrayC[i] = s.arrayA[i] * 0.5f + s.arrayB[i] * 0.5f;
    }
    for (int i = 1; i < STRESS_ARRAY_SIZE - 1; i++) {
        s.arrayA[i] = (s.arrayC[i-1] + s.arrayC[i] + s.arrayC[i+1]) / 3.0f;
    }

    // ── 8. I/O 读写（ProcessImage 访问） ──
    s.ioCycle++;
    REAL piInput = PI_READ_INPUT(io, REAL, sizeof(REAL) * 0);
    (void)piInput;
    PI_WRITE_OUTPUT(io, REAL, sizeof(REAL) * 0, feedback);
    PI_WRITE_OUTPUT(io, REAL, sizeof(REAL) * 1, filtered);
    PI_WRITE_OUTPUT(io, DWORD, sizeof(REAL) * 2, s.packedBits);
    PI_WRITE_OUTPUT(io, DWORD, sizeof(REAL) * 2 + sizeof(DWORD), (DWORD)s.stateMachine.currentState);
    PI_WRITE_OUTPUT(io, REAL, sizeof(REAL) * 2 + sizeof(DWORD) * 2, (REAL)s.cycleIndex);

    // ── 9. GVL 共享变量访问 ──
    REAL gvlVal = gvl.read<REAL>(0);
    gvl.write<REAL>(0, gvlVal + 1.0f);
    gvl.write<UDINT>(sizeof(REAL), (UDINT)s.cycleIndex);
    gvl.write<DWORD>(sizeof(REAL) + sizeof(UDINT), s.packedBits);
}


// ═══════════════════════════════════════════════════════
// 注册 POU
// ═══════════════════════════════════════════════════════

void registerPOU_StressTest(POURegistry& reg) {
    POUCallbacks cbs;
    cbs.init   = stressInit;
    cbs.cyclic = stressCyclic;
    reg.add("StressTest", cbs);
}
