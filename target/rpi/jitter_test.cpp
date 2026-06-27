/**
 * jitter_test.cpp — 纯 clock_nanosleep 抖动测试
 *
 * 编译：g++ -O2 -DRT_PLATFORM_LINUX jitter_test.cpp -lpthread -o jitter_test
 * 运行：taskset -c 3 sudo ./jitter_test --cycle-us 1000
 */
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <csignal>
#include <ctime>
#include <cstdint>
#include <unistd.h>
#include <sys/mman.h>
#include <pthread.h>

static volatile bool running = true;
static void sig_handler(int) { running = false; }

int main(int argc, char* argv[]) {
    int64_t cycleUs = 1000;
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--cycle-us") == 0 && i + 1 < argc) {
            cycleUs = atol(argv[i + 1]);
            i++;
        }
    }

    signal(SIGINT, sig_handler);
    signal(SIGTERM, sig_handler);

    // RT 优先级
    struct sched_param param = { .sched_priority = 99 };
    sched_setscheduler(0, SCHED_FIFO, &param);
    mlockall(MCL_CURRENT | MCL_FUTURE);

    printf("clock_nanosleep jitter test: cycle=%lldus\n", (long long)cycleUs);
    printf("Press Ctrl+C to stop\n\n");

    struct timespec next;
    clock_gettime(CLOCK_MONOTONIC, &next);

    int64_t jitter_min = 999999, jitter_max = 0, jitter_sum = 0;
    uint64_t count = 0;
    int64_t last_print = 0;

    while (running) {
        clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &next, nullptr);

        int64_t now_ns = (int64_t)next.tv_sec * 1000000000LL + next.tv_nsec;

        // 推进下一个周期
        next.tv_nsec += cycleUs * 1000;
        while (next.tv_nsec >= 1000000000L) {
            next.tv_sec++;
            next.tv_nsec -= 1000000000L;
        }

        // 计算实际间隔
        struct timespec ts;
        clock_gettime(CLOCK_MONOTONIC, &ts);
        int64_t actual_ns = (int64_t)ts.tv_sec * 1000000000LL + ts.tv_nsec;
        int64_t delta_us = (actual_ns - now_ns) / 1000;

        if (count > 0 && delta_us > 0 && delta_us < 100000) {
            jitter_sum += delta_us;
            if (delta_us < jitter_min) jitter_min = delta_us;
            if (delta_us > jitter_max) jitter_max = delta_us;
            count++;
        } else {
            count++;
        }

        // 每 2 秒输出一次
        if (count > 0 && (int64_t)count - last_print >= 2000 / (cycleUs / 1000)) {
            last_print = count;
            printf("  samples=%llu  min=%lldus  max=%lldus  avg=%lldus\n",
                   (unsigned long long)count,
                   (long long)jitter_min,
                   (long long)jitter_max,
                   (long long)(jitter_sum / (count > 1 ? count - 1 : 1)));
        }
    }

    printf("\n=== Final Results ===\n");
    printf("  Samples : %llu\n", (unsigned long long)count);
    printf("  Min     : %lld us\n", (long long)jitter_min);
    printf("  Max     : %lld us\n", (long long)jitter_max);
    printf("  Avg     : %lld us\n", (long long)(jitter_sum / (count > 1 ? count - 1 : 1)));
    return 0;
}
