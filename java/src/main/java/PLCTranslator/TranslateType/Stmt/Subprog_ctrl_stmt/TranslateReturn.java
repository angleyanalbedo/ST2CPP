package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt;

import antlr4.PLCSTPARSERParser;
import PLCTranslator.PLCTranslatorNew;

public class TranslateReturn {
    public String translateNode(PLCSTPARSERParser.ReturnContext ctx, PLCTranslatorNew translatorNew){
        return "\n\treturn;";
    }
}
