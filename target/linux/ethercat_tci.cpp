/**
 * ethercat_tci.cpp — EtherCAT TCI 实现（Linux）
 *
 * POSIX 平台：usleep、unistd.h。
 * 需要 libpcap 提供底层包捕获。
 */
#include "ethercat_tci.h"

extern "C" {
#include "soem/soem.h"
}

#include <cstdio>
#include <cstring>
#include <unistd.h>

using rt_plc::TCI;
using rt_plc::ProcessImage;
using rt_plc::BOOL;

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
        usleep(10000);
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

void EthercatTCI::syncInputs(ProcessImage& img) {
    int wkc = ecx_receive_processdata(&ctx, EC_TIMEOUTRET);
    if (wkc == 0 && m_operational)
        fprintf(stderr, "[EtherCAT] WKC=0, comm lost\n");

    for (int i = 0; i < ECAT_INPUT_MAP_SIZE; i++) {
        const PdoMapping& m = ECAT_INPUT_MAP[i];
        if (m.slaveIdx < 1 || m.slaveIdx > m_slaveCount) continue;
        uint8* slaveInput = ctx.slavelist[m.slaveIdx].inputs;
        if (!slaveInput) continue;

        if (m.plcBitOff >= 0) {
            BOOL val = (slaveInput[m.pdoByteOff] & (1 << m.plcBitOff)) ? TRUE : FALSE;
            img.writeInputBit(m.plcByteOff, m.plcBitOff, val);
        } else if (m.plcByteOff + m.sizeBytes <= PROCESS_IMAGE_SIZE) {
            memcpy(img.inputs + m.plcByteOff, slaveInput + m.pdoByteOff, m.sizeBytes);
        }
    }
}

void EthercatTCI::syncOutputs(ProcessImage& img) {
    for (int i = 0; i < ECAT_OUTPUT_MAP_SIZE; i++) {
        const PdoMapping& m = ECAT_OUTPUT_MAP[i];
        if (m.slaveIdx < 1 || m.slaveIdx > m_slaveCount) continue;
        uint8* slaveOutput = ctx.slavelist[m.slaveIdx].outputs;
        if (!slaveOutput) continue;

        if (m.plcBitOff >= 0) {
            BOOL val = img.readOutputBit(m.plcByteOff, m.plcBitOff);
            if (val) slaveOutput[m.pdoByteOff] |=  (1 << m.plcBitOff);
            else     slaveOutput[m.pdoByteOff] &= ~(1 << m.plcBitOff);
        } else if (m.plcByteOff + m.sizeBytes <= PROCESS_IMAGE_SIZE) {
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
