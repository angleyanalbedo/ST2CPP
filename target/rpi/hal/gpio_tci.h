/**
 * gpio_tci.h — GPIO ↔ PLC ProcessImage 同步桥接
 *
 * 实现 TCI 接口，在每个 PLC 扫描周期：
 *   syncInputs():  物理 GPIO → ProcessImage.inputs[]
 *   syncOutputs(): ProcessImage.outputs[] → 物理 GPIO
 *
 * 映射表在运行时从 JSON 配置加载，或使用默认映射。
 *
 * 默认映射（BCM GPIO → PLC 地址）：
 *   Inputs:
 *     BCM 5  → %IX0.0  (物理 Pin 29)
 *     BCM 6  → %IX0.1  (物理 Pin 31)
 *     BCM 13 → %IX0.2  (物理 Pin 33)
 *     BCM 19 → %IX0.3  (物理 Pin 35)
 *     BCM 26 → %IX0.4  (物理 Pin 37)
 *     BCM 21 → %IX0.5  (物理 Pin 40)
 *     BCM 20 → %IX0.6  (物理 Pin 38)
 *     BCM 16 → %IX0.7  (物理 Pin 36)
 *   Outputs:
 *     BCM 17 → %QX0.0  (物理 Pin 11)
 *     BCM 27 → %QX0.1  (物理 Pin 13)
 *     BCM 22 → %QX0.2  (物理 Pin 15)
 *     BCM 23 → %QX0.3  (物理 Pin 16)
 *     BCM 24 → %QX0.4  (物理 Pin 18)
 *     BCM 25 → %QX0.5  (物理 Pin 22)
 *     BCM 12 → %QX0.6  (物理 Pin 32)
 *     BCM 18 → %QX0.7  (物理 Pin 12)
 *
 * 用法：
 *   RpiGpioTCI tci;
 *   tci.init();                        // 初始化 GPIO HAL + 加载映射
 *   Runtime rt;
 *   rt.setTCI(&tci);                   // 注册到运行时
 *   // 之后 rt.scanOnce() 会自动调用 tci.syncInputs/syncOutputs
 */

#ifndef GPIO_TCI_H
#define GPIO_TCI_H

#include "rt_plc.h"

// BCM GPIO 最大编号
#ifndef GPIO_TCI_MAX_PINS
#define GPIO_TCI_MAX_PINS 28
#endif

// 每个 I/O 方向最多支持的字节数（1 字节 = 8 个位）
#ifndef GPIO_TCI_MAX_BYTES
#define GPIO_TCI_MAX_BYTES 8
#endif

struct GpioPinMapping {
    int  bcmPin;     // BCM GPIO 编号，-1 表示未映射
    int  byteOffset; // PLC I/O 字节偏移（%IB0 → 0, %IB1 → 1, ...）
    int  bitOffset;  // 位偏移（0-7）
    bool isInput;    // true=输入, false=输出
};

class RpiGpioTCI : public rt_plc::TCI {
public:
    RpiGpioTCI();
    ~RpiGpioTCI() override;

    /**
     * 初始化 GPIO HAL 并加载默认映射。
     * @return 0 成功, -1 失败
     */
    int init();

    /**
     * 从文件加载引脚映射（JSON 格式）。
     * 格式：
     * {
     *   "inputs": [
     *     {"bcm": 5, "byte": 0, "bit": 0},
     *     {"bcm": 6, "byte": 0, "bit": 1}
     *   ],
     *   "outputs": [
     *     {"bcm": 17, "byte": 0, "bit": 0},
     *     {"bcm": 27, "byte": 0, "bit": 1}
     *   ]
     * }
     */
    int loadMapping(const char* jsonPath);

    /**
     * 手动添加一个引脚映射。
     */
    void addInputMapping(int bcmPin, int byteOffset, int bitOffset);
    void addOutputMapping(int bcmPin, int byteOffset, int bitOffset);

    /**
     * 清空所有映射。
     */
    void clearMappings();

    // TCI 接口实现
    void syncInputs(rt_plc::ProcessImage& img) override;
    void syncOutputs(rt_plc::ProcessImage& img) override;

    /** 打印当前映射表 */
    void dumpMapping() const;

    /** 获取映射数量 */
    int inputCount() const { return m_inputCount; }
    int outputCount() const { return m_outputCount; }

private:
    GpioPinMapping m_inputs[GPIO_TCI_MAX_PINS];
    GpioPinMapping m_outputs[GPIO_TCI_MAX_PINS];
    int m_inputCount;
    int m_outputCount;

    void loadDefaultMapping();
};

#endif // GPIO_TCI_H
