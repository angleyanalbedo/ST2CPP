/**
 * pdo_config.h — EtherCAT PDO ↔ ProcessImage 映射配置
 *
 * 编译期静态配置：定义每个 EtherCAT PDO 条目映射到 ProcessImage 的字节/位。
 *
 * 用法：
 *   ST 代码中  AT %IW0  : INT;  对应 ProcessImage.inputs[0..1]
 *   pdo_config.h 中配置 {slaveIdx=1, pdoByteOff=0, plcByteOff=0, plcBitOff=-1, sizeBytes=2}
 *
 * 修改此文件即可适配不同的从站拓扑，无需改运行时代码。
 */
#pragma once

struct PdoMapping {
    int slaveIdx;       // 从站编号（1-based）
    int pdoByteOff;     // PDO 内字节偏移
    int plcByteOff;     // ProcessImage 字节偏移
    int plcBitOff;      // ProcessImage 位偏移（0-7，-1 表示多字节）
    int sizeBytes;      // 数据大小（字节）
};

// ═══════════════════════════════════════════════════════
// 示例配置：单个 EtherCAT 伺服驱动器
//
// TXPDO（从站→主站，PLC 输入）：
//   StatusWord      2B → %IW0 (offset 0)
//   ActualVelocity  2B → %IW1 (offset 2)
//   ActualPosition  4B → %ID2 (offset 4)
//
// RXPDO（主站→从站，PLC 输出）：
//   ControlWord     2B → %QW0 (offset 0)
//   TargetVelocity  2B → %QW1 (offset 2)
//   TargetPosition  4B → %QD2 (offset 4)
// ═══════════════════════════════════════════════════════

static const PdoMapping ECAT_INPUT_MAP[] = {
    // { slaveIdx, pdoByteOff, plcByteOff, plcBitOff, sizeBytes }
    { 1, 0,  0, -1, 2 },   // StatusWord      → %IW0
    { 1, 2,  2, -1, 2 },   // ActualVelocity  → %IW1
    { 1, 4,  4, -1, 4 },   // ActualPosition  → %ID2
};
static const int ECAT_INPUT_MAP_SIZE = sizeof(ECAT_INPUT_MAP) / sizeof(ECAT_INPUT_MAP[0]);

static const PdoMapping ECAT_OUTPUT_MAP[] = {
    { 1, 0,  0, -1, 2 },   // ControlWord     → %QW0
    { 1, 2,  2, -1, 2 },   // TargetVelocity  → %QW1
    { 1, 4,  4, -1, 4 },   // TargetPosition  → %QD2
};
static const int ECAT_OUTPUT_MAP_SIZE = sizeof(ECAT_OUTPUT_MAP) / sizeof(ECAT_OUTPUT_MAP[0]);

// 单个 PDO 条目的最大数量
#ifndef ECAT_MAX_PDO_ENTRIES
#define ECAT_MAX_PDO_ENTRIES 64
#endif
