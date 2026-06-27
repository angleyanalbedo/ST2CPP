package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateSuperCall {
    public String translateNode(PLCSTPARSERParser.SuperCallContext ctx, PLCTranslatorNew translatorNew){
        String methodName = ctx.derived_func_name().getText();
        return "\n\t\t// SUPER::" + methodName + "(this); /* FIXME: resolve parent class */";
    }
}
