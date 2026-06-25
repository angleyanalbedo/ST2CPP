#pragma once

#include "task.h"
#include <cstring>

namespace rt_plc {

/**
 * POURegistry — POU 函数指针注册表
 *
 * 编译器生成的代码通过 add() 注册每个 PROGRAM 函数，
 * 运行时通过 lookup() 按名字查找，用于 tasks.json 调度配置。
 *
 * 多文件场景：每个 .st 文件编译后产出一个 registerPOU_XXX() 函数，
 * 构建脚本生成的 pou_registry.gen.cpp 汇总所有 registerPOU_XXX() 调用。
 */
class POURegistry {
public:
    static constexpr int MAX_ENTRIES = 64;

    void add(const char* name, POUFunc func) {
        if (count_ < MAX_ENTRIES) {
            entries_[count_].name = name;
            entries_[count_].func = func;
            count_++;
        }
    }

    POUFunc lookup(const char* name) const {
        for (int i = 0; i < count_; i++) {
            if (std::strcmp(entries_[i].name, name) == 0) {
                return entries_[i].func;
            }
        }
        return nullptr;
    }

    int count() const { return count_; }

    struct Entry {
        const char* name;
        POUFunc     func;
    };

    const Entry* entries() const { return entries_; }

private:
    Entry entries_[MAX_ENTRIES] = {};
    int   count_ = 0;
};

} // namespace rt_plc
