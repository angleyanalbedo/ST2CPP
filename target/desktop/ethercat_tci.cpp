/**
 * ethercat_tci.cpp — EtherCAT TCI 实现（跨平台桌面版）
 *
 * 通过 _WIN32 宏分支 Windows/MinGW（Sleep）与 POSIX（usleep）。
 * 适合开发机直接运行 EtherCAT，无需区分 linux/windows 目标。
 *
 * 注意：types.h（经 ethercat_tci.h → rt_plc.h 引入）必须在 <windows.h>
 * 之前 include，避免 ERROR/TRUE/FALSE 宏污染。
 * 本文件不 using namespace rt_plc;，以避免 BOOL/DWORD/INT/UINT 歧义；
 * 所有 rt_plc 类型均显式使用 rt_plc:: 前缀。
 */
#include "ethercat_tci.h"

extern "C" {
#include "soem/soem.h"
}

#include <cstdio>
#include <cstring>

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#define SLEEP_MS(ms) Sleep(ms)
#else
#include <unistd.h>
#define SLEEP_MS(ms) usleep((ms) * 1000)
#endif

static ecx_contextt ctx;

EthercatTCI::EthercatTCI()
    : m_operational(false), m_slaveCount(0)
{
    memset(IOmap, 0, sizeof(IOmap));
    m_ifname[0] = '\0';
}

EthercatTCI::~EthercatTCI() { shutdown(); }

int EthercatTCI::init(const char* ifname) {
    strncpy(m_ifname, ifname, sizeof(m_ifname) - 1);
    m_ifname[sizeof(m_ifname) - 1] = '\0';
    fprintf(stderr, "[EtherCAT] Initializing on %s...\n", ifname);

    memset(&ctx, 0, sizeof(ctx));
    if (ecx_init(&ctx, ifname) <= 0) {
        fprintf(stderr, "[EtherCAT] ecx_init(%s) failed\n", ifname);
        return -1;
    }
    fprintf(stderr, "[EtherCAT] Network interface opened\n");

    m_slaveCount = ecx_config_init(&ctx);
    if (m_slaveCount <= 0) {
        fprintf(stderr, "[EtherCAT] No slaves found\n");
        ecx_close(&ctx);
        m_ifname[0] = '\0';
        return -1;
    }
    fprintf(stderr, "[EtherCAT] Found %d slave(s)\n", m_slaveCount);

    for (int i = 1; i <= m_slaveCount; i++)
        fprintf(stderr, "  Slave %d: %s\n", i, ctx.slavelist[i].name);

    int wkc = ecx_config_map_group(&ctx, IOmap, 0);
    fprintf(stderr, "[EtherCAT] PDO mapping: WKC=%d\n", wkc);

    ecx_configdc(&ctx);

    fprintf(stderr, "[EtherCAT] Waiting for OPERATIONAL state...\n");
    ctx.slavelist[0].state = EC_STATE_OPERATIONAL;
    ecx_writestate(&ctx, 0);

    int timeout = 100;
    while (timeout-- > 0) {
        ecx_readstate(&ctx);
        bool allOp = true;
        for (int i = 1; i <= m_slaveCount; i++) {
            if (ctx.slavelist[i].state != EC_STATE_OPERATIONAL) { allOp = false; break; }
        }
        if (allOp) break;
        SLEEP_MS(10);
    }

    if (ctx.slavelist[0].state == EC_STATE_OPERATIONAL) {
        m_operational = true;
        fprintf(stderr, "[EtherCAT] All slaves OPERATIONAL\n");
    } else {
        fprintf(stderr, "[EtherCAT] WARNING: Not all slaves reached OP\n");
        m_operational = false;
    }
    return 0;
}

void EthercatTCI::shutdown() {
    if (m_ifname[0] != '\0' && m_slaveCount > 0) {
        if (m_operational) {
            ctx.slavelist[0].state = EC_STATE_SAFE_OP;
            ecx_writestate(&ctx, 0);
        }
        ecx_close(&ctx);
        fprintf(stderr, "[EtherCAT] Connection closed\n");
        m_ifname[0] = '\0';
    }
    m_operational = false;
    m_slaveCount = 0;
}

void EthercatTCI::syncInputs(rt_plc::ProcessImage& img) {
    int wkc = ecx_receive_processdata(&ctx, EC_TIMEOUTRET);
    if (wkc == 0 && m_operational)
        fprintf(stderr, "[EtherCAT] WKC=0, comm lost\n");

    for (int i = 0; i < ECAT_INPUT_MAP_SIZE; i++) {
        const PdoMapping& m = ECAT_INPUT_MAP[i];
        if (m.slaveIdx < 1 || m.slaveIdx > m_slaveCount) continue;
        uint8* slaveInput = ctx.slavelist[m.slaveIdx].inputs;
        if (!slaveInput) continue;

        if (m.plcBitOff >= 0) {
            rt_plc::BOOL val = (slaveInput[m.pdoByteOff] & (1 << m.plcBitOff)) ? TRUE : FALSE;
            img.writeInputBit(m.plcByteOff, m.plcBitOff, val);
        } else if (m.plcByteOff + m.sizeBytes <= rt_plc::PROCESS_IMAGE_SIZE) {
            memcpy(img.inputs + m.plcByteOff, slaveInput + m.pdoByteOff, m.sizeBytes);
        }
    }
}

void EthercatTCI::syncOutputs(rt_plc::ProcessImage& img) {
    for (int i = 0; i < ECAT_OUTPUT_MAP_SIZE; i++) {
        const PdoMapping& m = ECAT_OUTPUT_MAP[i];
        if (m.slaveIdx < 1 || m.slaveIdx > m_slaveCount) continue;
        uint8* slaveOutput = ctx.slavelist[m.slaveIdx].outputs;
        if (!slaveOutput) continue;

        if (m.plcBitOff >= 0) {
            rt_plc::BOOL val = img.readOutputBit(m.plcByteOff, m.plcBitOff);
            if (val) slaveOutput[m.pdoByteOff] |=  (1 << m.plcBitOff);
            else     slaveOutput[m.pdoByteOff] &= ~(1 << m.plcBitOff);
        } else if (m.plcByteOff + m.sizeBytes <= rt_plc::PROCESS_IMAGE_SIZE) {
            memcpy(slaveOutput + m.pdoByteOff, img.outputs + m.plcByteOff, m.sizeBytes);
        }
    }
    ecx_send_processdata(&ctx);
}

bool EthercatTCI::isOperational() const { return m_operational; }
int EthercatTCI::slaveCount() const { return m_slaveCount; }

void EthercatTCI::printDiagnostics() const {
    fprintf(stderr, "\n=== EtherCAT Diagnostics ===\n");
    fprintf(stderr, "Interface   : %s\n", m_ifname);
    fprintf(stderr, "Slaves      : %d\n", m_slaveCount);
    fprintf(stderr, "Operational : %s\n", m_operational ? "YES" : "NO");
    fprintf(stderr, "PDO In/Out  : %d / %d entries\n", ECAT_INPUT_MAP_SIZE, ECAT_OUTPUT_MAP_SIZE);
    for (int i = 1; i <= m_slaveCount; i++) {
        fprintf(stderr, "  Slave %d: %s state=0x%02x DC=%s\n",
                i, ctx.slavelist[i].name, ctx.slavelist[i].state,
                ctx.slavelist[i].hasdc ? "yes" : "no");
    }
    fprintf(stderr, "===========================\n\n");
}
