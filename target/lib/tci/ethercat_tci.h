/**
 * ethercat_tci.h — EtherCAT TCI 实现（桌面跨平台版）
 *
 * 通过 SOEM 将 EtherCAT PDO 映射到 PLC ProcessImage。
 */
#pragma once

#include "rt_plc.h"
#include "pdo_config.h"

class EthercatTCI : public rt_plc::TCI {
public:
    EthercatTCI();
    ~EthercatTCI() override;

    int init(const char* ifname);
    void shutdown();

    void syncInputs(rt_plc::ProcessImage& img) override;
    void syncOutputs(rt_plc::ProcessImage& img) override;

    bool isOperational() const;
    int slaveCount() const;
    void printDiagnostics() const;

private:
    uint8_t IOmap[4096];
    bool m_operational;
    int m_slaveCount;
    char m_ifname[32];
};
