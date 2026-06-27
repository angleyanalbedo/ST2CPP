package PLCTranslator.TranslateType.Stmt.Iteration_stmt.While_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateWhile_stmt {
    public String translateNode(PLCSTPARSERParser.While_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\twhile(").append(PLCTranslatorNew.translateExpression(ctx.expression(), translatorNew)).append("){");
        String bodyResult = translatorNew.visit(ctx.stmt_list());
        if (bodyResult != null) {
            sb.append(bodyResult);
        }
        sb.append("\n\t\t}");
        return sb.toString();
    }
}
