package PLCTranslator;

import PLCTranslator.TranslateType.packageFactory;

import java.util.List;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

/**
 * OOP 后端 — 生成 PLC_Value 风格的 C++ 代码
 *
 * 使用旧版运行时 PLC.h，变量是堆上的对象，支持在线调试。
 * 从现有 TranslateXxx 的 writeTarget() 调用中提取。
 */
public class OOPCodeGenerator implements CodeGenerator {

    private final packageFactory factory = new packageFactory();

    // ═══ 文件头尾 ═══

    @Override
    public void emitHeader() {
        writeTarget("#include <iostream>"
                + "\n#include \"PLC.h\""
                + "\nusing namespace PLC;"
                + "\nusing namespace std;"
        );
    }

    @Override
    public void emitFooter() {
        // OOP 模式下由 Main.java 调用 closeWriter()
    }


    // ═══ 变量声明 ═══

    @Override
    public void emitVarDecl(String name, String typeName, String assignVar) {
        writeTarget("\n\t\tauto* " + name + " = new " + typeName + "(" + assignVar + ");");
    }

    @Override
    public void emitGlobalVarDecl(String name, String typeName, String assignVar, String varSection) {
        writeTarget("\n\t" + factory.packagePROGVarDeclSentences(name, typeName, assignVar));
    }

    @Override
    public void emitProgVarDecl(String name, String typeName, String assignVar) {
        writeTarget("\n\t" + factory.packagePROGVarDeclSentences(name, typeName, assignVar));
    }


    // ═══ 赋值 ═══

    @Override
    public void emitAssign(String varName, String exprAssignVar) {
        writeTarget("\n\t\t" + varName + " = " + exprAssignVar + ";");
    }

    @Override
    public void emitFuncReturnAssign(String exprAssignVar) {
        writeTarget("\n\t\t" + "*this->returnValue = " + exprAssignVar + ";");
    }


    // ═══ 控制流 ═══

    @Override
    public void emitIfBegin(String condAssignVar) {
        writeTarget("\n\t\tif(" + condAssignVar + "){");
    }

    @Override
    public void emitElseIf(String condAssignVar) {
        writeTarget("\n\t\t}else if(" + condAssignVar + "){");
    }

    @Override
    public void emitElse() {
        writeTarget("\n\t\t}else{");
    }

    @Override
    public void emitIfEnd() {
        writeTarget("\n\t\t}");
    }

    @Override
    public void emitForBegin(String controlVar, String fromAssignVar, String toAssignVar, String stepAssignVar) {
        writeTarget("\n\t\tfor( *" + controlVar + " = " + fromAssignVar + ";");
        writeTarget("*" + controlVar + " <= " + toAssignVar + ";");
        if (stepAssignVar != null && !stepAssignVar.isEmpty()) {
            writeTarget("*" + controlVar + " = *" + controlVar + " + " + stepAssignVar + "){");
        } else {
            writeTarget(controlVar + " = " + controlVar + " + (*new INT(1))){");
        }
    }

    @Override
    public void emitForEnd() {
        writeTarget("\n\t\t}");
    }

    @Override
    public void emitWhileBegin(String condAssignVar) {
        writeTarget("\n\t\twhile(" + condAssignVar + "){");
    }

    @Override
    public void emitWhileEnd() {
        writeTarget("\n\t\t}");
    }

    @Override
    public void emitRepeatBegin() {
        writeTarget("\n\t\tdo{");
    }

    @Override
    public void emitRepeatEnd(String condAssignVar) {
        writeTarget("\n\t\t}while(" + condAssignVar + ");");
    }

    @Override
    public void emitCaseBegin(String exprAssignVar) {
        writeTarget("\n\t\tswitch(" + exprAssignVar + "){");
    }

    @Override
    public void emitCaseOption(String value) {
        writeTarget("\n\t\t\tcase " + value + " :");
    }

    @Override
    public void emitCaseDefault() {
        writeTarget("\n\t\t\tdefault :");
    }

    @Override
    public void emitCaseEnd() {
        writeTarget("\n\t\t}");
    }

    @Override
    public void emitPrintStmt(String exprAssignVar) {
        writeTarget("\n\t\tcout << " + exprAssignVar + " << endl;");
    }


    // ═══ 函数 / PROGRAM ═══

    @Override
    public void emitFuncDeclBegin(String funcName, String returnType, String params) {
        writeTarget("\nclass " + funcName + " : public PLC_Function<" + returnType + ">{");
        writeTarget("\n\tpublic:");
    }

    @Override
    public void emitFuncDeclEnd() {
        writeTarget("\n};");
    }

    @Override
    public void emitFuncBodyBegin() {
        // OOP 模式下函数体在 funcExecute 方法中
    }

    @Override
    public void emitFuncBodyEnd() {
        // OOP 模式下由 TranslateFunc_decl 处理
    }

    @Override
    public void emitFuncCall(String funcName, List<String> params) {
        // OOP 模式下由 TranslateFunc_call 处理
    }

    @Override
    public void emitProgDeclBegin(String progName) {
        // OOP 模式下由 TranslateProg_decl 处理
    }

    @Override
    public void emitProgDeclEnd() {
        writeTarget("\n}");
    }

    @Override
    public void emitProgBodyBegin() {
        // OOP 模式下由 TranslateProg_decl 处理
    }

    @Override
    public void emitProgBodyEnd() {
        // OOP 模式下由 TranslateProg_decl 处理
    }


    // ═══ 函数块 ═══

    @Override
    public void emitFBBodyBegin() {
        // 由 TranslateFb_body 处理
    }

    @Override
    public void emitFBBodyEnd() {
        // 由 TranslateFb_body 处理
    }


    // ═══ 初始化 ═══

    @Override
    public void emitInitFuncBegin() {
        writeTarget("\nvoid initFunc(){");
    }

    @Override
    public void emitInitFuncEnd() {
        writeTarget("\n}");
    }

    @Override
    public void emitInitFuncCall(String sentence) {
        writeTarget("\n\t" + sentence);
    }


    // ═══ 底层输出 ═══

    @Override
    public void write(String code) {
        writeTarget(code);
    }
}
