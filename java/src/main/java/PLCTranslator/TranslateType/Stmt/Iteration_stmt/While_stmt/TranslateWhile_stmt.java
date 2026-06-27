package PLCTranslator.TranslateType.Stmt.Iteration_stmt.While_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译while循环
 */

public class TranslateWhile_stmt {
    public String translateNode(PLCSTPARSERParser.While_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        //翻译while循环语句
        PLCVariable varExpression = PLCTranslatorNew.getVariable(ctx.expression(), "while expression");

        sb.append("\n\t\twhile(").append(PLCTranslatorNew.gvlCtx.translateExpr(varExpression.getAssignVar())).append("){");
        String bodyResult = translatorNew.visit(ctx.stmt_list());
        if (bodyResult != null) {
            sb.append(bodyResult);
        }
        sb.append("\n\t\t}");
        return sb.toString();
    }
}
