package PLCTranslator.TranslateType.Stmt.Assign_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateVariableAssignExpression {
    public String translateNode(PLCSTPARSERParser.VariableAssignExpressionContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        // 访问表达式以收集函数调用产生的临时变量声明
        translatorNew.visit(ctx.expression());
        // 统一输出 pending 的临时变量声明
        for (String decl : translatorNew.pendingDecls) {
            sb.append(decl);
        }
        translatorNew.pendingDecls.clear();

        PLCVariable varSymbol = PLCTranslatorNew.getVariable(ctx, "assign target");

        if(varSymbol.getSort() != PLCModifierEnum.Sort.FC) {
            String translated = PLCTranslatorNew.gvlCtx.translateExpr(varSymbol.getAssignVar());
            sb.append("\n\t\t").append(PLCTranslatorNew.gvlCtx.writeExpr(varSymbol.getName(), translated)).append(";");
        }else{
            String translated = translatorNew.visit(ctx.expression());
            sb.append("\n\t\treturn ").append(translated).append(";");
        }
        return sb.toString();
    }
}
