#pragma once

#include "rt_plc.h"

#include <atomic>
#include <cstdint>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

namespace rt_plc {

class NetworkTCI : public TCI {
public:
    static constexpr int MAX_BYTE_MAPPINGS = 32;
    static constexpr int NET_BUF_SIZE = 4096;

    struct ByteMapping {
        int plcOffset;
        int netOffset;
        int length;
    };

    NetworkTCI();
    ~NetworkTCI() override;

    NetworkTCI(const NetworkTCI&) = delete;
    NetworkTCI& operator=(const NetworkTCI&) = delete;

    /**
     * Connect to a remote TCP server.
     * Starts background I/O thread.
     * @param host  IP or hostname
     * @param port  TCP port
     * @param reconnectMs  auto-reconnect interval (0 = no reconnect)
     * @return true if connection initiated (not necessarily established)
     */
    bool begin(const char* host, uint16_t port, int reconnectMs = 3000);

    /**
     * Start in server mode — listen for incoming connections.
     * Starts background accept + I/O thread.
     */
    bool begin(uint16_t port, int reconnectMs = 3000);

    /**
     * Disconnect and stop background thread.
     */
    void end();

    bool isRunning() const { return m_running.load(std::memory_order_acquire); }
    bool isConnected() const { return m_connected.load(std::memory_order_acquire); }

    // ─── Mapping: PLC ProcessImage ↔ Network buffer ───

    void mapInput(int plcOffset, int netOffset, int length);
    void mapOutput(int plcOffset, int netOffset, int length);
    void clearMappings();

    int inputMappingCount() const { return m_inputCount; }
    int outputMappingCount() const { return m_outputCount; }

    // ─── TCI interface (non-blocking, called from PLC cycle) ───

    void syncInputs(ProcessImage& img) override;
    void syncOutputs(ProcessImage& img) override;

    // ─── Diagnostics ───

    uint64_t rxBytes() const { return m_rxBytes.load(std::memory_order_relaxed); }
    uint64_t txBytes() const { return m_txBytes.load(std::memory_order_relaxed); }
    uint32_t reconnectCount() const { return m_reconnectCount.load(std::memory_order_relaxed); }

private:
    void workerClient(std::string host, uint16_t port);
    void workerServer(uint16_t port);
    void workerLoop(int sock);

    // Mapping tables
    ByteMapping m_inputMaps[MAX_BYTE_MAPPINGS];
    ByteMapping m_outputMaps[MAX_BYTE_MAPPINGS];
    int m_inputCount;
    int m_outputCount;

    // Network buffer (shared between worker ↔ sync)
    mutable std::mutex m_mutex;
    std::vector<uint8_t> m_rxBuf;
    std::vector<uint8_t> m_txBuf;
    bool m_txPending;

    // Background I/O thread
    std::thread m_worker;
    std::atomic<bool> m_running{false};
    std::atomic<bool> m_connected{false};
    int m_reconnectMs;
    bool m_serverMode;

    // Stats
    std::atomic<uint64_t> m_rxBytes{0};
    std::atomic<uint64_t> m_txBytes{0};
    std::atomic<uint32_t> m_reconnectCount{0};
};

} // namespace rt_plc
