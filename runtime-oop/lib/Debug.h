#ifndef DEBUG_H
#define DEBUG_H

#include <iostream>
#include <string>
#include <sstream>

// 定义调试级别
enum DebugLevel {
    DEBUG_NONE = 0,  // 不输出任何调试信息
    DEBUG_INFO,      // 输出普通信息
    DEBUG_WARNING,   // 输出警告信息
    DEBUG_ERROR,     // 输出错误信息
    DEBUG_ALL        // 输出所有信息
};

// 设置调试级别
#ifndef DEBUG_LEVEL
#define DEBUG_LEVEL DEBUG_INFO
#endif

// 定义调试宏
#define DEBUG(level, ...) do { \
    if ((level) <= DEBUG_LEVEL) { \
        std::ostringstream oss; \
        oss << __VA_ARGS__; \
        std::cerr << "[" << __FILE__ << ":" << __LINE__ << "] " << oss.str() << std::endl; \
    } \
} while (0)

// 定义不同级别的调试宏
#define DEBUG_INFO(...) DEBUG(DEBUG_INFO, __VA_ARGS__)
#define DEBUG_WARNING(...) DEBUG(DEBUG_WARNING, __VA_ARGS__)
#define DEBUG_ERROR(...) DEBUG(DEBUG_ERROR, __VA_ARGS__)

#endif // DEBUG_H