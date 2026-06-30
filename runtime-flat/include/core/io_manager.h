#pragma once

#include "rt_plc.h"

namespace rt_plc {

class IoManager {
public:
    void setTCI(TCI* tci);
    TCI* tci() const;

    void syncInputs(ProcessImage& image);
    void syncOutputs(ProcessImage& image);

    void enableSafeOutputs(bool enabled);
    bool safeOutputsEnabled() const;
    void clearSafeOutputs();
    void setSafeOutputByte(size_t offset, uint8_t value);
    void applySafeOutputs(ProcessImage& image) const;

private:
    TCI*    tci_ = nullptr;
    bool    safeOutputsEnabled_ = false;
    uint8_t safeOutputs_[PROCESS_IMAGE_SIZE] = {};
};

} // namespace rt_plc
