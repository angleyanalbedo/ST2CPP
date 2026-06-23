package PLCTranslator;

import java.util.List;

/**
 * 代码生成器接口 — 支持 OOP 和 Flat 两种后端
 *
 * OOP 后端：生成 PLC_Value 风格代码（调试/教学用）
 * Flat 后端：生成 GVL 偏移量风格代码（生产/性能用）
 */
public interface CodeGenerator {

    // ═══ 文件头尾 ═══

    /** 生成 #include 和 using 声明 */
    void emitHeader();

    /** 生成文件尾（关闭 writer 等） */
    void emitFooter();


    // ═══ 变量声明 ═══

    /**
     * 生成局部变量声明
     * OOP:  auto* x = new INT(0);
     * Flat: // 偏移量已分配，无需声明
     */
    void emitVarDecl(String name, String typeName, String assignVar);

    /**
     * 生成全局变量声明
     * OOP:  auto* x = new INT(0);
     * Flat: // 偏移量已分配，无需声明
     */
    void emitGlobalVarDecl(String name, String typeName, String assignVar, String varSection);

    /**
     * 生成 PROGRAM 内变量声明
     */
    void emitProgVarDecl(String name, String typeName, String assignVar);


    // ═══ 赋值 ═══

    /**
     * 生成变量赋值
     * OOP:  counter = counter + 1;
     * Flat: gvl.write<INT>(0, gvl.read<INT>(0) + 1);
     */
    void emitAssign(String varName, String exprAssignVar);

    /**
     * 生成函数返回值赋值
     * OOP:  *this->returnValue = expr;
     * Flat: return expr;
     */
    void emitFuncReturnAssign(String exprAssignVar);


    // ═══ 控制流 ═══

    void emitIfBegin(String condAssignVar);
    void emitElseIf(String condAssignVar);
    void emitElse();
    void emitIfEnd();

    void emitForBegin(String controlVar, String fromAssignVar, String toAssignVar, String stepAssignVar);
    void emitForEnd();

    void emitWhileBegin(String condAssignVar);
    void emitWhileEnd();

    void emitRepeatBegin();
    void emitRepeatEnd(String condAssignVar);

    void emitCaseBegin(String exprAssignVar);
    void emitCaseOption(String value);
    void emitCaseDefault();
    void emitCaseEnd();

    void emitPrintStmt(String exprAssignVar);


    // ═══ 函数 / PROGRAM ═══

    /**
     * 生成函数声明开始
     * OOP:  class FuncName : public PLC_Function<ReturnType> {
     * Flat: ReturnType FuncName(参数列表) {
     */
    void emitFuncDeclBegin(String funcName, String returnType, String params);

    /** 生成函数声明结束 */
    void emitFuncDeclEnd();

    /**
     * 生成函数体开始（包含局部变量初始化）
     */
    void emitFuncBodyBegin();

    /** 生成函数体结束（包含返回值输出） */
    void emitFuncBodyEnd();

    /**
     * 生成函数调用
     * OOP:  FuncName::callFunc(...)
     * Flat: FuncName(...)
     */
    void emitFuncCall(String funcName, List<String> params);

    /**
     * 生成 PROGRAM 声明开始
     * OOP:  int main() { initFunc();
     * Flat: void PROGRAM_Name(GVL& gvl, ProcessImage& io, TIME dt) {
     */
    void emitProgDeclBegin(String progName);

    /** 生成 PROGRAM 声明结束 */
    void emitProgDeclEnd();

    /** 生成 PROGRAM 体开始（变量声明之后，语句之前） */
    void emitProgBodyBegin();

    /** 生成 PROGRAM 体结束 */
    void emitProgBodyEnd();


    // ═══ 函数块 ═══

    void emitFBBodyBegin();
    void emitFBBodyEnd();


    // ═══ 初始化 ═══

    /** 生成 initFunc() 函数（注册所有函数到 RFM） */
    void emitInitFuncBegin();
    void emitInitFuncEnd();
    void emitInitFuncCall(String sentence);


    // ═══ 底层输出 ═══

    /** 直接输出代码片段（用于特殊情况） */
    void write(String code);
}
