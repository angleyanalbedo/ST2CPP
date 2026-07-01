#include "core/retain_manager.h"

namespace rt_plc {

RetainManager::RetainManager(GVL& gvl)
    : gvl_(gvl) {
}

bool RetainManager::setRegion(size_t start, size_t end) {
    if (start > end || end > GVL_SIZE) {
        RT_LOG_ERR("[RetainManager] invalid retain region: [%zu, %zu)\n", start, end);
        return false;
    }

    gvl_.setRetainRegion(start, end);
    return true;
}

void RetainManager::clearForStartup(StartupMode mode) {
    if (mode == StartupMode::COLD) {
        gvl_.clear();
        invalidate();
        return;
    }

    restore();
    gvl_.clearNonRetain();
}

void RetainManager::save() {
    gvl_.saveRetain();
}

bool RetainManager::restore() {
    if (!gvl_.retainDirty) {
        return false;
    }

    gvl_.loadRetain();
    return true;
}

void RetainManager::invalidate() {
    gvl_.retainDirty = false;
}

size_t RetainManager::start() const {
    return gvl_.retainStart;
}

size_t RetainManager::end() const {
    return gvl_.retainEnd;
}

size_t RetainManager::size() const {
    return hasRegion() ? gvl_.retainEnd - gvl_.retainStart : 0;
}

bool RetainManager::hasRegion() const {
    return gvl_.retainStart < gvl_.retainEnd;
}

bool RetainManager::hasBackup() const {
    return gvl_.retainDirty;
}

} // namespace rt_plc
