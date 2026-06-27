/**
 * ethercat_tci.h — EtherCAT TCI 实现
 *
 * 通过 SOEM 将 EtherCAT PDO 映射到 PLC ProcessImage。
 * 实现 TCI 接口：syncInputs() 从从站读取输入，syncOutputs() 向从站写入输出。
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
