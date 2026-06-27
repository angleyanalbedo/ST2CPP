package PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译else if语句
 */
public class TranslateElsif_stmt {
    public String translateNode(PLCSTPARSERParser.Elsif_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        PLCVariable varExpression = PLCTranslatorNew.getVariable(ctx.expression(), "elsif expression");
        sb.append("\n\t\t}else if(").append(PLCTranslatorNew.gvlCtx.translateExpr(varExpression.getAssignVar())).append("){");
        String stmtListResult = translatorNew.visit(ctx.stmt_list());
        if (stmtListResult != null) {
            sb.append(stmtListResult);
        }
        return sb.toString();
    }
}
