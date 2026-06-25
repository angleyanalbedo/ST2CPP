package PLCTranslator;

import java.util.List;

/**
 * 代码生成器接口 — Flat 后端
 *
 * 生成 GVL 偏移量风格代码（生产/性能用）。
 * 所有 emit 方法返回生成的代码字符串（而非直接写文件），
 * 由顶层调度器统一收集后一次性写入文件。
 */
public interface CodeGenerator {

    // ═══ 文件头尾 ═══

    /** 生成 #include 和 using 声明 */
    String emitHeader();

    /** 生成文件尾（关闭 writer 等） */
    String emitFooter();


    // ═══ 变量声明 ═══

    /**
     * 生成局部变量声明
     */
    String emitVarDecl(String name, String typeName, String assignVar);

    /**
     * 生成全局变量声明
     */
    String emitGlobalVarDecl(String name, String typeName, String assignVar, String varSection);

    /**
     * 生成 PROGRAM 内变量声明
     */
    String emitProgVarDecl(String name, String typeName, String assignVar);


    // ═══ 赋值 ═══

    /**
     * 生成变量赋值
     */
    String emitAssign(String varName, String exprAssignVar);

    /**
     * 生成函数返回值赋值
     */
    String emitFuncReturnAssign(String exprAssignVar);


    // ═══ 控制流 ═══

    String emitIfBegin(String condAssignVar);
    String emitElseIf(String condAssignVar);
    String emitElse();
    String emitIfEnd();

    String emitForBegin(String controlVar, String fromAssignVar, String toAssignVar, String stepAssignVar);
    String emitForEnd();

    String emitWhileBegin(String condAssignVar);
    String emitWhileEnd();

    String emitRepeatBegin();
    String emitRepeatEnd(String condAssignVar);

    String emitCaseBegin(String exprAssignVar);
    String emitCaseOption(String value);
    String emitCaseDefault();
    String emitCaseEnd();

    String emitPrintElement(String translatedExpr, boolean isString);


    // ═══ 函数 / PROGRAM ═══

    /**
     * 生成函数声明开始
     */
    String emitFuncDeclBegin(String funcName, String returnType, String params);

    /** 生成函数声明结束 */
    String emitFuncDeclEnd();

    /**
     * 生成函数体开始（包含局部变量初始化）
     */
    String emitFuncBodyBegin();

    /** 生成函数体结束（包含返回值输出） */
    String emitFuncBodyEnd();

    /**
     * 生成函数调用
     */
    String emitFuncCall(String funcName, List<String> params);

    /**
     * 生成 PROGRAM 声明开始
     */
    String emitProgDeclBegin(String progName);

    /** 生成 PROGRAM 声明结束 */
    String emitProgDeclEnd();

    /** 生成 PROGRAM 体开始（变量声明之后，语句之前） */
    String emitProgBodyBegin();

    /** 生成 PROGRAM 体结束 */
    String emitProgBodyEnd();


    // ═══ 函数块 ═══

    String emitFBBodyBegin();
    String emitFBBodyEnd();


    // ═══ 初始化 ═══

    /** 生成 initFunc() 函数（注册所有函数到 RFM） */
    String emitInitFuncBegin();
    String emitInitFuncEnd();
    String emitInitFuncCall(String sentence);


    // ═══ 表达式转换 ═══

    /**
     * 将静态检查层生成的中间表达式转换为 Flat 后端的表达式。
     *
     * 中间表达式示例：
     *   - 字面量：(*(new INT(2))) → 2
     *   - 变量引用：gvl.read<TYPE>(offset)
     *   - 函数调用：FUN(p1, p2)
     *   - 复合表达式：(1 + 2)
     *
     * @param expr 静态检查层生成的中间表达式
     * @return Flat 后端的表达式
     */
    String translateExpr(String expr);


    // ═══ 底层输出 ═══

    /** 直接输出代码片段（用于特殊情况） */
    String write(String code);
}
