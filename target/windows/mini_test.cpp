#include "tcp_tci.h"
#include <cstdio>
#include <thread>
int main() {
    printf("start\n");
    fflush(stdout);
    rt_plc::TcpTCI tcp("127.0.0.1", 19999, 0);
    printf("constructed\n");
    fflush(stdout);
    for (int i = 0; i < 50 && !tcp.isConnected(); i++) {
        printf("wait %d\n", i);
        fflush(stdout);
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    printf("done: connected=%d\n", tcp.isConnected());
    fflush(stdout);
    return 0;
}
