#pragma once

#include "network_tci.h"

namespace rt_plc {

/**
 * TcpTCI — TCP 网络 TCI 便捷包装
 *
 * 比 NetworkTCI 更简洁的用法，覆盖俩常见场景：
 *   客户端: TcpTCI tcp("host", port);
 *   服务端: TcpTCI tcp(port);
 *   对称映射: tcp.map(plcOffset, length);
 *
 * 内部持有 NetworkTCI，所有 I/O 由后台线程处理，PLC 周期不阻塞。
 */
class TcpTCI : public TCI {
public:
    /** 客户端模式：连远端 host:port */
    TcpTCI(const char* host, uint16_t port, int reconnectMs = 3000);

    /** 服务端模式：监听本地 port */
    TcpTCI(uint16_t port, int reconnectMs = 3000);

    ~TcpTCI() override;

    TcpTCI(const TcpTCI&) = delete;
    TcpTCI& operator=(const TcpTCI&) = delete;

    /** 对称映射：%IW(offset)/%QW(offset) ↔ 网络 byte (offset)，length 字节 */
    void map(int offset, int length);

    /** 输入/输出偏移不同时使用 */
    void mapInput(int plcOffset, int netOffset, int length);
    void mapOutput(int plcOffset, int netOffset, int length);

    /** 清空所有映射 */
    void clearMappings();

    // ─── TCI 接口 ───
    void syncInputs(ProcessImage& img) override;
    void syncOutputs(ProcessImage& img) override;

    // ─── 诊断 ───
    bool isConnected() const;
    uint64_t rxBytes() const;
    uint64_t txBytes() const;
    uint32_t reconnectCount() const;

private:
    NetworkTCI m_net;
};

} // namespace rt_plc
