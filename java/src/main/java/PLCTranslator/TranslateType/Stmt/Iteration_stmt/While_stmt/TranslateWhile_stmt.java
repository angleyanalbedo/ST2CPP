package PLCTranslator.TranslateType.Stmt.Iteration_stmt.While_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateWhile_stmt {
    public String translateNode(PLCSTPARSERParser.While_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        String condResult = translatorNew.visit(ctx.expression());
        for (String decl : translatorNew.pendingDecls) {
            sb.append(decl);
        }
        translatorNew.pendingDecls.clear();
        sb.append("\n\t\twhile(").append(condResult).append("){");
        String bodyResult = translatorNew.visit(ctx.stmt_list());
        if (bodyResult != null) {
            sb.append(bodyResult);
        }
        sb.append("\n\t\t}");
        return sb.toString();
    }
}
