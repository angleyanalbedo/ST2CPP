package PLCTranslator.TranslateType.Stmt.Iteration_stmt.Repeat_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译repeat循环语句
 */
public class TranslateRepeat_stmt {
    public String translateNode(PLCSTPARSERParser.Repeat_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
       sb.append("\n\t\tdo{");
       String bodyResult = translatorNew.visit(ctx.stmt_list());
       if (bodyResult != null) {
           sb.append(bodyResult);
       }

       PLCVariable varExpression = PLCTranslatorNew.getVariable(ctx.expression(), "repeat expression");
       sb.append("\n\t\t}while(!(").append(PLCTranslatorNew.gvlCtx.translateExpr(varExpression.getAssignVar())).append("));");
       return sb.toString();
    }
}
