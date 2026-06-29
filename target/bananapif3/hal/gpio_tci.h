#ifndef GPIO_TCI_H
#define GPIO_TCI_H

#include "rt_plc.h"

#ifndef GPIO_TCI_MAX_PINS
#define GPIO_TCI_MAX_PINS 32
#endif

#ifndef GPIO_TCI_MAX_BYTES
#define GPIO_TCI_MAX_BYTES 8
#endif

struct GpioPinMapping {
    int  gpioLine;
    int  byteOffset;
    int  bitOffset;
    bool isInput;
};

class BpiGpioTCI : public rt_plc::TCI {
public:
    BpiGpioTCI();
    ~BpiGpioTCI() override;

    int init();

    void addInputMapping(int gpioLine, int byteOffset, int bitOffset);
    void addOutputMapping(int gpioLine, int byteOffset, int bitOffset);
    void clearMappings();

    void syncInputs(rt_plc::ProcessImage& img) override;
    void syncOutputs(rt_plc::ProcessImage& img) override;

    void dumpMapping() const;

    int inputCount() const { return m_inputCount; }
    int outputCount() const { return m_outputCount; }

private:
    GpioPinMapping m_inputs[GPIO_TCI_MAX_PINS];
    GpioPinMapping m_outputs[GPIO_TCI_MAX_PINS];
    int m_inputCount;
    int m_outputCount;

    void loadDefaultMapping();
};

#endif
