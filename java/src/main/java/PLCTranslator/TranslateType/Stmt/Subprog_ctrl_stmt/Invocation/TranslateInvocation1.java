package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.Invocation;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 *  翻译类或方法块实例方法函数调用
 */
public class TranslateInvocation1 {
    public String translateNode(PLCSTPARSERParser.Invocation1Context ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        PLCVariable instanceSymbol = PLCTranslatorNew.getVariable(ctx.invocation1branch(), "invocation1 instance");

        String methodName = ctx.method_name().getText();

        for (PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()) {
            PLCVariable paraSymbol = PLCTranslatorNew.getVariable(param_assignContext, "invocation1 parameter");
        }
        return sb.toString();
    }
}
