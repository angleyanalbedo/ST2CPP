package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.Invocation;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译功能块和类对象调用
 */
public class TranslateInvocation2 {
    public String translateNode(PLCSTPARSERParser.Invocation2Context ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        PLCVariable instanceSymbol = PLCTranslatorNew.getVariable(ctx.invocation2branch(), "invocation2 instance");

        for (PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()) {
            PLCVariable paraSymbol = PLCTranslatorNew.getVariable(param_assignContext, "invocation2 parameter");
        }
        return sb.toString();
    }
}
