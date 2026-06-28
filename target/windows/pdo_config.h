/**
 * pdo_config.h — EtherCAT PDO ↔ ProcessImage 映射配置（Windows 版）
 */
#pragma once

struct PdoMapping {
    int slaveIdx;       // 从站编号（1-based）
    int pdoByteOff;     // PDO 内字节偏移
    int plcByteOff;     // ProcessImage 字节偏移
    int plcBitOff;      // 位偏移（0-7，-1=多字节）
    int sizeBytes;      // 数据大小（字节）
};

static const PdoMapping ECAT_INPUT_MAP[] = {
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

#ifndef ECAT_MAX_PDO_ENTRIES
#define ECAT_MAX_PDO_ENTRIES 64
#endif
