package PLCTranslator.TranslateType.Stmt_list;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateStmt_list {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Stmt_listContext ctx, PLCTranslatorNew translatorNew){
        for (PLCSTPARSERParser.StmtContext stmtContext : ctx.stmt()) {
            translatorNew.visit(stmtContext);
        }

        return null;
    }
}
