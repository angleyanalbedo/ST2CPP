package PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateElsif_stmt {
    public String translateNode(PLCSTPARSERParser.Elsif_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\t}else if(").append(translatorNew.visit(ctx.expression())).append("){");
        String stmtListResult = translatorNew.visit(ctx.stmt_list());
        if (stmtListResult != null) {
            sb.append(stmtListResult);
        }
        return sb.toString();
    }
}
