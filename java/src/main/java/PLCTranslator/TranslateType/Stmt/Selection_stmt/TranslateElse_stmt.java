package PLCTranslator.TranslateType.Stmt.Selection_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译else语句
 */
public class TranslateElse_stmt {
    public String translateNode(PLCSTPARSERParser.Else_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\t}else{");
        String stmtListResult = translatorNew.visit(ctx.stmt_list());
        if (stmtListResult != null) {
            sb.append(stmtListResult);
        }
        return sb.toString();
    }
}
