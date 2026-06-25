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
        PLCVariable controlVariable = PLCTranslatorNew.getVariable(ctx.control_variable(), "for control variable");
        String conVar = controlVariable.getName();
        PLCVariable varExpression1 = PLCTranslatorNew.getVariable(ctx.expression(0), "for start expression");

        PLCVariable varExpression2 = PLCTranslatorNew.getVariable(ctx.expression(1), "for end expression");

        String stepVar = null;
        if(ctx.by_list()!=null){
            PLCVariable varStepSize = PLCTranslatorNew.getVariable(ctx.by_list(), "for step expression");
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
