#pragma once

#include "program.h"
#include <cstring>

namespace rt_plc {

/**
 * POURegistry — POU 回调注册表
 *
 * 编译器生成的代码通过 add() 注册每个 PROGRAM 的 Init/Cyclic/Pre/Post 回调，
 * 运行时通过 lookup() 按名字查找，用于 tasks.json 调度配置。
 *
 * 多文件场景：每个 .st 文件编译后产出一个 registerPOU_XXX() 函数，
 * 构建脚本生成的 pou_registry.gen.cpp 汇总所有 registerPOU_XXX() 调用。
 */
struct POUCallbacks {
    PRG_InitFunc   init   = nullptr;
    PRG_PreFunc    pre    = nullptr;
    PRG_CyclicFunc cyclic = nullptr;
    PRG_PostFunc   post   = nullptr;
};

class POURegistry {
public:
    static constexpr int MAX_ENTRIES = 64;

    void add(const char* name, const POUCallbacks& cbs) {
        if (count_ < MAX_ENTRIES) {
            entries_[count_].name = name;
            entries_[count_].cbs = cbs;
            count_++;
        }
    }

    void add(const char* name, PRG_InitFunc init, PRG_CyclicFunc cyclic,
             PRG_PreFunc pre = nullptr, PRG_PostFunc post = nullptr) {
        POUCallbacks cbs;
        cbs.init = init;
        cbs.cyclic = cyclic;
        cbs.pre = pre;
        cbs.post = post;
        add(name, cbs);
    }

    const POUCallbacks* lookup(const char* name) const {
        for (int i = 0; i < count_; i++) {
            if (std::strcmp(entries_[i].name, name) == 0) {
                return &entries_[i].cbs;
            }
        }
        return nullptr;
    }

    int count() const { return count_; }

    struct Entry {
        const char*  name;
        POUCallbacks cbs;
    };

    const Entry* entries() const { return entries_; }

private:
    Entry entries_[MAX_ENTRIES] = {};
    int   count_ = 0;
};

} // namespace rt_plc
