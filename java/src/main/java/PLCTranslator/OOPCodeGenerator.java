package PLCTranslator;

import PLCTranslator.TranslateType.packageFactory;

import java.util.List;

/**
 * OOP 后端 — 生成 PLC_Value 风格的 C++ 代码
 *
 * 使用旧版运行时 PLC.h，变量是堆上的对象，支持在线调试。
 * 从现有 TranslateXxx 的 writeTarget() 调用中提取。
 *
 * 所有 emit 方法返回代码字符串（不再直接写文件）。
 */
public class OOPCodeGenerator implements CodeGenerator {

    private final packageFactory factory = new packageFactory();

    // ═══ 文件头尾 ═══

    @Override
    public String emitHeader() {
        return "#include <iostream>"
                + "\n#include \"PLC.h\""
                + "\nusing namespace PLC;"
                + "\nusing namespace std;"
        ;
    }

    @Override
    public String emitFooter() {
        return ""; // OOP 模式下由 Main.java 调用 closeWriter()
    }


    // ═══ 变量声明 ═══

    @Override
    public String emitVarDecl(String name, String typeName, String assignVar) {
        return "\n\t\tauto* " + name + " = new " + typeName + "(" + assignVar + ");";
    }

    @Override
    public String emitGlobalVarDecl(String name, String typeName, String assignVar, String varSection) {
        return "\n\t" + factory.packagePROGVarDeclSentences(name, typeName, assignVar);
    }

    @Override
    public String emitProgVarDecl(String name, String typeName, String assignVar) {
        return "\n\t" + factory.packagePROGVarDeclSentences(name, typeName, assignVar);
    }


    // ═══ 赋值 ═══

    @Override
    public String emitAssign(String varName, String exprAssignVar) {
        return "\n\t\t" + varName + " = " + exprAssignVar + ";";
    }

    @Override
    public String emitFuncReturnAssign(String exprAssignVar) {
        return "\n\t\t" + "*this->returnValue = " + exprAssignVar + ";";
    }


    // ═══ 控制流 ═══

    @Override
    public String emitIfBegin(String condAssignVar) {
        return "\n\t\tif(" + condAssignVar + "){";
    }

    @Override
    public String emitElseIf(String condAssignVar) {
        return "\n\t\t}else if(" + condAssignVar + "){";
    }

    @Override
    public String emitElse() {
        return "\n\t\t}else{";
    }

    @Override
    public String emitIfEnd() {
        return "\n\t\t}";
    }

    @Override
    public String emitForBegin(String controlVar, String fromAssignVar, String toAssignVar, String stepAssignVar) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\tfor( *").append(controlVar).append(" = ").append(fromAssignVar).append(";");
        sb.append("*").append(controlVar).append(" <= ").append(toAssignVar).append(";");
        if (stepAssignVar != null && !stepAssignVar.isEmpty()) {
            sb.append("*").append(controlVar).append(" = *").append(controlVar).append(" + ").append(stepAssignVar).append("){");
        } else {
            sb.append(controlVar).append(" = ").append(controlVar).append(" + (*new INT(1))){");
        }
        return sb.toString();
    }

    @Override
    public String emitForEnd() {
        return "\n\t\t}";
    }

    @Override
    public String emitWhileBegin(String condAssignVar) {
        return "\n\t\twhile(" + condAssignVar + "){";
    }

    @Override
    public String emitWhileEnd() {
        return "\n\t\t}";
    }

    @Override
    public String emitRepeatBegin() {
        return "\n\t\tdo{";
    }

    @Override
    public String emitRepeatEnd(String condAssignVar) {
        return "\n\t\t}while(" + condAssignVar + ");";
    }

    @Override
    public String emitCaseBegin(String exprAssignVar) {
        return "\n\t\tswitch(" + exprAssignVar + "){";
    }

    @Override
    public String emitCaseOption(String value) {
        return "\n\t\t\tcase " + value + " :";
    }

    @Override
    public String emitCaseDefault() {
        return "\n\t\t\tdefault :";
    }

    @Override
    public String emitCaseEnd() {
        return "\n\t\t}";
    }

    @Override
    public String emitPrintStmt(String exprAssignVar) {
        return "\n\t\tcout << " + exprAssignVar + " << endl;";
    }


    // ═══ 函数 / PROGRAM ═══

    @Override
    public String emitFuncDeclBegin(String funcName, String returnType, String params) {
        return "\nclass " + funcName + " : public PLC_Function<" + returnType + ">{"
                + "\n\tpublic:";
    }

    @Override
    public String emitFuncDeclEnd() {
        return "\n};";
    }

    @Override
    public String emitFuncBodyBegin() {
        return ""; // OOP 模式下函数体在 funcExecute 方法中
    }

    @Override
    public String emitFuncBodyEnd() {
        return ""; // OOP 模式下由 TranslateFunc_decl 处理
    }

    @Override
    public String emitFuncCall(String funcName, List<String> params) {
        return ""; // OOP 模式下由 TranslateFunc_call 处理
    }

    @Override
    public String emitProgDeclBegin(String progName) {
        return ""; // OOP 模式下由 TranslateProg_decl 处理
    }

    @Override
    public String emitProgDeclEnd() {
        return "\n}";
    }

    @Override
    public String emitProgBodyBegin() {
        return ""; // OOP 模式下由 TranslateProg_decl 处理
    }

    @Override
    public String emitProgBodyEnd() {
        return ""; // OOP 模式下由 TranslateProg_decl 处理
    }


    // ═══ 函数块 ═══

    @Override
    public String emitFBBodyBegin() {
        return ""; // 由 TranslateFb_body 处理
    }

    @Override
    public String emitFBBodyEnd() {
        return ""; // 由 TranslateFb_body 处理
    }


    // ═══ 初始化 ═══

    @Override
    public String emitInitFuncBegin() {
        return "\nvoid initFunc(){";
    }

    @Override
    public String emitInitFuncEnd() {
        return "\n}";
    }

    @Override
    public String emitInitFuncCall(String sentence) {
        return "\n\t" + sentence;
    }


    // ═══ 表达式转换 ═══

    @Override
    public String translateExpr(String oopExpr) {
        return oopExpr; // OOP 模式：直通，不做转换
    }


    // ═══ 底层输出 ═══

    @Override
    public String write(String code) {
        return code;
    }
}
