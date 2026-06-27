/**
 * ethercat_tci.h — EtherCAT TCI 实现
 *
 * 通过 SOEM 将 EtherCAT PDO 映射到 PLC ProcessImage。
 * 实现 TCI 接口：syncInputs() 从从站读取输入，syncOutputs() 向从站写入输出。
 *
 * 用法：
 *   EthercatTCI ecat;
 *   ecat.init("eth0");
 *   sched.setTCI(&ecat);
 */
#pragma once

#include "rt_plc.h"
#include "pdo_config.h"

class EthercatTCI : public rt_plc::TCI {
public:
    EthercatTCI();
    ~EthercatTCI() override;

    /**
     * 初始化 SOEM 并进入 OP 状态。
     * @param ifname 网口名（如 "eth0"）
     * @return 0 成功, -1 失败
     */
    int init(const char* ifname);

    /** 关闭 SOEM 连接 */
    void shutdown();

    // TCI 接口
    void syncInputs(rt_plc::ProcessImage& img) override;
    void syncOutputs(rt_plc::ProcessImage& img) override;

    /** 是否所有从站已进入 OP 状态 */
    bool isOperational() const;

    /** 已发现的从站数量 */
    int slaveCount() const;

    /** 打印从站诊断信息 */
    void printDiagnostics() const;

private:
    uint8_t IOmap[4096];
    bool m_operational;
    int m_slaveCount;
    char m_ifname[32];
};
