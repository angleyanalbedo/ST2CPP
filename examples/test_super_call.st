(* test_super_call.st — 测试 Bug 4: SUPER:: 调用
 *
 * 问题：SUPER.MethodName() 只输出注释，不生成实际父类调用
 * 预期：SUPER. 应翻译为 ParentClass::MethodName(this, ...) 的 C++ 调用
 * 注意：ST 标准语法是 SUPER.method() 而非 SUPER::method()
 *)

CLASS BASE_CTRL
    VAR
        BASE_STATE : INT := 0;
    END_VAR

    METHOD INIT_BASE : INT
        BASE_STATE := 1;
        INIT_BASE := BASE_STATE;
    END_METHOD
END_CLASS

CLASS CHILD_CTRL EXTENDS BASE_CTRL
    VAR
        CHILD_DATA : INT := 0;
    END_VAR

    METHOD INIT_CHILD : INT
        SUPER.INIT_BASE();
        CHILD_DATA := 42;
        INIT_CHILD := CHILD_DATA;
    END_METHOD
END_CLASS

PROGRAM TEST_SUPER
    VAR
        Child : CHILD_CTRL;
        R     : INT := 0;
    END_VAR

    R := Child.INIT_CHILD();
    ASSERT(R = 42);
END_PROGRAM
