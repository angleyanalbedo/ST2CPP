package PLCTranslator.TranslateType.Stmt.Iteration_stmt.Repeat_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateRepeat_stmt {
    public String translateNode(PLCSTPARSERParser.Repeat_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\tdo{");
        String bodyResult = translatorNew.visit(ctx.stmt_list());
        if (bodyResult != null) {
            sb.append(bodyResult);
        }
        String condResult = translatorNew.visit(ctx.expression());
        for (String decl : translatorNew.pendingDecls) {
            sb.append(decl);
        }
        translatorNew.pendingDecls.clear();
        sb.append("\n\t\t}while(!(").append(condResult).append("));");
        return sb.toString();
    }
}
