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
            // 使用 CodeGenerator 生成赋值
            sb.append(PLCTranslatorNew.codeGen.emitAssign(varSymbol.getName(), varSymbol.getAssignVar()));
        }else{
            // 函数返回值赋值
            sb.append(PLCTranslatorNew.codeGen.emitFuncReturnAssign(varExpression.getAssignVar()));
        }
        return sb.toString();

    }
}
