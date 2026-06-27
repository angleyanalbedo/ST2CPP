package PLCTranslator.TranslateType.Stmt.Assign_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateVariableAssignExpression {
    public String translateNode(PLCSTPARSERParser.VariableAssignExpressionContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        PLCVariable varSymbol = PLCTranslatorNew.getVariable(ctx, "assign target");

        if(varSymbol.getSort() != PLCModifierEnum.Sort.FC) {
            String translated = PLCTranslatorNew.gvlCtx.translateExpr(varSymbol.getAssignVar());
            sb.append("\n\t\t").append(PLCTranslatorNew.gvlCtx.writeExpr(varSymbol.getName(), translated)).append(";");
        }else{
            String translated = PLCTranslatorNew.translateExpression(ctx.expression(), translatorNew);
            sb.append("\n\t\treturn ").append(translated).append(";");
        }
        return sb.toString();
    }
}
