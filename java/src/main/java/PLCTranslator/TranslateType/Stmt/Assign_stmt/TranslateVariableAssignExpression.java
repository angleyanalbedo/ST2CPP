package PLCTranslator.TranslateType.Stmt.Assign_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

/**
 * 翻译PLCSt的变量赋值语句
 */
public class TranslateVariableAssignExpression {
    public ArrayList<String> translateNode(PLCSTPARSERParser.VariableAssignExpressionContext ctx, PLCTranslatorNew translatorNew){
        translatorNew.visit(ctx.expression());
        PLCVariable varSymbol = (PLCVariable) PLCTranslatorNew.properties.get(ctx).get(0);

        PLCVariable varExpression = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression()).get(0);

        if(varSymbol.getSort() != PLCModifierEnum.Sort.FC) {
            writeTarget("\n\t\t" + varSymbol.getName() + " = " + varSymbol.getAssignVar()+ ";");
        }else{
            writeTarget("\n\t\t" + "*this->returnValue = "+varExpression.getAssignVar()+";");
        }
        return null;

    }
}
