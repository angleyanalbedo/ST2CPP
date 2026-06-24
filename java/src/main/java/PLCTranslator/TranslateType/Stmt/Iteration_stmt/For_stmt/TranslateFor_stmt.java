package PLCTranslator.TranslateType.Stmt.Iteration_stmt.For_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.Objects;

public class TranslateFor_stmt {
    packageFactory pFactory = new packageFactory();
    public String translateNode(PLCSTPARSERParser.For_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        //*********************************************翻译for循环条件****************************************************
        PLCVariable controlVariable = (PLCVariable) PLCTranslatorNew.properties.get(ctx.control_variable()).get(0);
        String conVar = controlVariable.getName();
        PLCVariable varExpression1 = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression(0)).get(0);

        //***********************************************翻译循环条件*****************************************************
        PLCVariable varExpression2 = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression(1)).get(0);

        //**********************************************
        String stepVar = null;
        if(ctx.by_list()!=null){
            PLCVariable varStepSize = (PLCVariable) PLCTranslatorNew.properties.get(ctx.by_list()).get(0);
            stepVar = varStepSize.getAssignVar();
        }

        // 使用 CodeGenerator 生成 for 循环
        sb.append(PLCTranslatorNew.codeGen.emitForBegin(conVar, varExpression1.getAssignVar(),
                varExpression2.getAssignVar(), stepVar));

        //翻译for循环体内操作
        String bodyResult = translatorNew.visit(ctx.stmt_list());
        if (bodyResult != null) {
            sb.append(bodyResult);
        }

        sb.append(PLCTranslatorNew.codeGen.emitForEnd());
        return sb.toString();
    }
}
