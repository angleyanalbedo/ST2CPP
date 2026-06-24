#pragma once

namespace rt_plc {

// SafetyModule 接口（预留）
// 未来可扩展：安全 I/O 处理、安全任务调度、安全通信等
class SafetyModule {
public:
    virtual ~SafetyModule() = default;

    virtual bool initialize() = 0;
    virtual void shutdown() = 0;
    virtual bool cyclicCheck() = 0;
    virtual bool isSafeState() const = 0;
};

} // namespace rt_plc
