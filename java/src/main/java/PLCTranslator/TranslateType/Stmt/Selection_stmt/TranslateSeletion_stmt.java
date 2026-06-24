package PLCTranslator.TranslateType.Stmt.Selection_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateSeletion_stmt {
    public String translateNode(PLCSTPARSERParser.Selection_stmtContext ctx, PLCTranslatorNew translatorNew){
        String result = translatorNew.visit(ctx.getChild(0));
        return result != null ? result : "";
    }
}
