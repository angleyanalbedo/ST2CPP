package PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译if选择语句
 */
public class TranslateIf_stmt {
    public String translateNode(PLCSTPARSERParser.If_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        PLCVariable varExpression = PLCTranslatorNew.getVariable(ctx.expression(), "if expression");

        sb.append("\n\t\tif(").append(PLCTranslatorNew.gvlCtx.translateExpr(varExpression.getAssignVar())).append("){");
        String stmtListResult = translatorNew.visit(ctx.stmt_list());
        if (stmtListResult != null) {
            sb.append(stmtListResult);
        }

        //翻译else if语句
        for (PLCSTPARSERParser.Elsif_stmtContext elsif_stmtContext : ctx.elsif_stmt()) {
            String elsifResult = translatorNew.visit(elsif_stmtContext);
            if (elsifResult != null) {
                sb.append(elsifResult);
            }
        }
        if(ctx.else_stmt()!=null){
            //翻译else语句
            String elseResult = translatorNew.visit(ctx.else_stmt());
            if (elseResult != null) {
                sb.append(elseResult);
            }
        }
        sb.append("\n\t\t}");
        return sb.toString();
    }
}
