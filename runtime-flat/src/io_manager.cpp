#include "core/io_manager.h"

#include <cstring>

namespace rt_plc {

void IoManager::setTCI(TCI* tci) {
    tci_ = tci;
}

TCI* IoManager::tci() const {
    return tci_;
}

void IoManager::syncInputs(ProcessImage& image) {
    if (tci_) tci_->syncInputs(image);
}

void IoManager::syncOutputs(ProcessImage& image) {
    if (tci_) tci_->syncOutputs(image);
}

void IoManager::enableSafeOutputs(bool enabled) {
    safeOutputsEnabled_ = enabled;
}

bool IoManager::safeOutputsEnabled() const {
    return safeOutputsEnabled_;
}

void IoManager::clearSafeOutputs() {
    memset(safeOutputs_, 0, sizeof(safeOutputs_));
}

void IoManager::setSafeOutputByte(size_t offset, uint8_t value) {
    if (offset >= PROCESS_IMAGE_SIZE) {
        RT_LOG_ERR("[IoManager] safe output byte out of bounds: offset=%zu\n", offset);
        return;
    }
    safeOutputs_[offset] = value;
}

void IoManager::applySafeOutputs(ProcessImage& image) const {
    if (!safeOutputsEnabled_) return;
    memcpy(image.outputs, safeOutputs_, PROCESS_IMAGE_SIZE);
}

} // namespace rt_plc
