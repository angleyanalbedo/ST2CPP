package PLCTranslator.TranslateType.Stmt.Assign_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译PLCSt的变量赋值语句
 */
public class TranslateVariableAssignExpression {
    public String translateNode(PLCSTPARSERParser.VariableAssignExpressionContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        String visitResult = translatorNew.visit(ctx.expression());
        if (visitResult != null) {
            sb.append(visitResult);
        }
        PLCVariable varSymbol = PLCTranslatorNew.getVariable(ctx, "assign target");

        PLCVariable varExpression = PLCTranslatorNew.getVariable(ctx.expression(), "assign expression");

        if(varSymbol.getSort() != PLCModifierEnum.Sort.FC) {
            String translated = PLCTranslatorNew.gvlCtx.translateExpr(varSymbol.getAssignVar());
            sb.append("\n\t\t").append(PLCTranslatorNew.gvlCtx.writeExpr(varSymbol.getName(), translated)).append(";");
        }else{
            String translated = PLCTranslatorNew.gvlCtx.translateExpr(varExpression.getAssignVar());
            sb.append("\n\t\treturn ").append(translated).append(";");
        }
        return sb.toString();

    }
}
