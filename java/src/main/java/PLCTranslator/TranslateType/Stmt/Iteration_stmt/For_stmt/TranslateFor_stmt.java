package PLCTranslator.TranslateType.Stmt.Iteration_stmt.For_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.Objects;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

public class TranslateFor_stmt {
    packageFactory pFactory = new packageFactory();
    public ArrayList<String> translateNode(PLCSTPARSERParser.For_stmtContext ctx, PLCTranslatorNew translatorNew){
        //*********************************************翻译for循环条件****************************************************
        PLCVariable controlVariable = (PLCVariable) PLCTranslatorNew.properties.get(ctx.control_variable()).get(0);
        String conVar = controlVariable.getName();
        PLCVariable varExpression1 = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression(0)).get(0);

        writeTarget("\n\tfor( *"+conVar+"="+
                varExpression1.getAssignVar() +";");
        //***********************************************翻译循环条件*****************************************************
        PLCVariable varExpression2 = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression(1)).get(0);

        writeTarget("*" + conVar+"<="+
                varExpression2.getAssignVar()+";");

        //**********************************************
        if(ctx.by_list()!=null){
            PLCVariable varStepSize = (PLCVariable) PLCTranslatorNew.properties.get(ctx.by_list()).get(0);
            writeTarget( "*" + conVar + " = *"+ conVar + "+" +
                    varStepSize.getAssignVar()+"){");
        }else{
            writeTarget(conVar+" = "+conVar+" + (*new INT(1))){");
        }

        //翻译for循环体内操作
        translatorNew.visit(ctx.stmt_list());

//        System.out.println("}");
        writeTarget("\n\t}");
        return null;
    }
}
