package PLCTranslator.TranslateType.Stmt.Selection_stmt.Case_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

/**
 * 翻译case语句，将case语句翻译成if <else if> <else> 句型
 */

public class TranslateCase_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Case_stmtContext ctx, PLCTranslatorNew translatorNew){
        PLCVariable varExpression = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression()).get(0);
        //***********************************************翻译第一个if****************************************************

        ArrayList<PLCSymbol> caseList = PLCTranslatorNew.properties.get(ctx.case_selection(0).case_list());
        // if((<expression> == <case>) <||(<expression> == <case>)> * )
//        System.out.println("if(("+varExpression.getAssignVar()+"=="+((PLCVariable)caseList.get(0)).getAssignVar()+
//                ")");

        writeTarget("\nif(("+varExpression.getAssignVar()+"=="+((PLCVariable)caseList.get(0)).getAssignVar()+
                ")");

        for(int t =1; t<ctx.case_selection(0).case_list().case_list_elem().size(); t++){
//            System.out.println("||("+varExpression.getAssignVar()+"=="+ ctx.case_selection(0).case_list().case_list_elem(t)+")");
            writeTarget("\n||("+varExpression.getAssignVar()+"=="
                    + ctx.case_selection(0).case_list().case_list_elem(t)+")");
        }

//        System.out.print("){");
        writeTarget("){");
        //翻译case操作内容
        translatorNew.visit(ctx.case_selection(0).stmt_list());

//        System.out.print("}");
        writeTarget("}");
        //**********************************************翻译后续case为else if*********************************************
        for(int i = 1; i<ctx.case_selection().size(); i++){
            ArrayList<PLCSymbol> caseTempList = PLCTranslatorNew.properties.get(ctx.case_selection(i).case_list());
//            System.out.println("else if(("+varExpression.getAssignVar()+"=="+((PLCVariable)caseTempList.get(0)).getAssignVar()+
//                    ")");
            writeTarget("\nelse if(("+varExpression.getAssignVar()+"=="+((PLCVariable)caseTempList.get(0)).getAssignVar()+
                    ")");

            for(int t =1; t<ctx.case_selection(i).case_list().case_list_elem().size(); t++){
//                System.out.println("||("+varExpression.getAssignVar()+"=="+ ctx.case_selection(i).case_list().case_list_elem(t)+")");
                writeTarget("\n||("+varExpression.getAssignVar()+"=="+ ctx.case_selection(i).case_list().case_list_elem(t)+")");
            }

//            System.out.println("){");
            writeTarget("\n){");
            translatorNew.visit(ctx.case_selection(i).stmt_list());

//            System.out.println("}");
            writeTarget("\n}");
        }

        //***********************************************翻译else语句****************************************************
        if(ctx.else_stmt() != null){
            translatorNew.visit(ctx.else_stmt());
        }else{
//            System.out.println("else{}");
            writeTarget("\nelse{}");
        }
        return null;
    }
}
