#include "tcp_tci.h"

namespace rt_plc {

// ─── 客户端模式 ───

TcpTCI::TcpTCI(const char* host, uint16_t port, int reconnectMs)
    : m_net()
{
    m_net.begin(host, port, reconnectMs);
}

// ─── 服务端模式 ───

TcpTCI::TcpTCI(uint16_t port, int reconnectMs)
    : m_net()
{
    m_net.begin(port, reconnectMs);
}

// ─── 析构 ───

TcpTCI::~TcpTCI() {
    m_net.end();
}

// ─── 映射 ───

void TcpTCI::map(int offset, int length) {
    m_net.mapInput(offset, offset, length);
    m_net.mapOutput(offset, offset, length);
}

void TcpTCI::mapInput(int plcOffset, int netOffset, int length) {
    m_net.mapInput(plcOffset, netOffset, length);
}

void TcpTCI::mapOutput(int plcOffset, int netOffset, int length) {
    m_net.mapOutput(plcOffset, netOffset, length);
}

void TcpTCI::clearMappings() {
    m_net.clearMappings();
}

// ─── TCI 接口 ───

void TcpTCI::syncInputs(ProcessImage& img) {
    m_net.syncInputs(img);
}

void TcpTCI::syncOutputs(ProcessImage& img) {
    m_net.syncOutputs(img);
}

// ─── 诊断 ───

bool TcpTCI::isConnected() const { return m_net.isConnected(); }
uint64_t TcpTCI::rxBytes() const { return m_net.rxBytes(); }
uint64_t TcpTCI::txBytes() const { return m_net.txBytes(); }
uint32_t TcpTCI::reconnectCount() const { return m_net.reconnectCount(); }

} // namespace rt_plc
