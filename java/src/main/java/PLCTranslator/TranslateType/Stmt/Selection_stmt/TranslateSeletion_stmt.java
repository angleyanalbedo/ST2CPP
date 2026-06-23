package PLCTranslator.TranslateType.Stmt.Selection_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateSeletion_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Selection_stmtContext ctx, PLCTranslatorNew translatorNew){
        translatorNew.visit(ctx.getChild(0));
        return null;
    }
}
