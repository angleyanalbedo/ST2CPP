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
        sb.append("\n\t\t}while(!(").append(PLCTranslatorNew.translateExpression(ctx.expression(), translatorNew)).append("));");
        return sb.toString();
    }
}
