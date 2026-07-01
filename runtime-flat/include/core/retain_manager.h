#pragma once

#include "gvl.h"
#include "types.h"
#include "constants.h"

namespace rt_plc {

class RetainManager {
public:
    explicit RetainManager(GVL& gvl);

    bool setRegion(size_t start, size_t end);
    void clearForStartup(StartupMode mode);

    void save();
    bool restore();
    void invalidate();

    size_t start() const;
    size_t end() const;
    size_t size() const;
    bool hasRegion() const;
    bool hasBackup() const;

private:
    GVL& gvl_;
};

} // namespace rt_plc
