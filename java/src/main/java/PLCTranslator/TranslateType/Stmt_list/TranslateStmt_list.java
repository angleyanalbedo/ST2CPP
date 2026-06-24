package PLCTranslator.TranslateType.Stmt_list;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateStmt_list {
    public String translateNode(PLCSTPARSERParser.Stmt_listContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        for (PLCSTPARSERParser.StmtContext stmtContext : ctx.stmt()) {
            String result = translatorNew.visit(stmtContext);
            if (result != null) {
                sb.append(result);
            }
        }
        return sb.toString();
    }
}
