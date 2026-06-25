package PLCTranslator.TranslateType.Stmt.Selection_stmt.Case_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

/**
 * 翻译case语句，将case语句翻译成if <else if> <else> 句型
 */

public class TranslateCase_stmt {
    public String translateNode(PLCSTPARSERParser.Case_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        PLCVariable varExpression = PLCTranslatorNew.getVariable(ctx.expression(), "case expression");
        //***********************************************翻译第一个if****************************************************

        ArrayList<PLCSymbol> caseList = PLCTranslatorNew.properties.get(ctx.case_selection(0).case_list());
        // if((<expression> == <case>) <||(<expression> == <case>)> * )
//        System.out.println("if(("+varExpression.getAssignVar()+"=="+((PLCVariable)caseList.get(0)).getAssignVar()+
//                ")");

        sb.append("\nif(("+translatorNew.codeGen.translateExpr(varExpression.getAssignVar())+"=="+translatorNew.codeGen.translateExpr(((PLCVariable)caseList.get(0)).getAssignVar())+
                ")");

        for(int t =1; t<ctx.case_selection(0).case_list().case_list_elem().size(); t++){
            sb.append("\n||("+translatorNew.codeGen.translateExpr(varExpression.getAssignVar())+"=="
                    + translatorNew.codeGen.translateExpr(ctx.case_selection(0).case_list().case_list_elem(t).getText())+")");
        }

//        System.out.print("){");
        sb.append("){");
        //翻译case操作内容
        String result = translatorNew.visit(ctx.case_selection(0).stmt_list());
        sb.append(result);

//        System.out.print("}");
        sb.append("}");
        //**********************************************翻译后续case为else if*********************************************
        for(int i = 1; i<ctx.case_selection().size(); i++){
            ArrayList<PLCSymbol> caseTempList = PLCTranslatorNew.properties.get(ctx.case_selection(i).case_list());
//            System.out.println("else if(("+varExpression.getAssignVar()+"=="+((PLCVariable)caseTempList.get(0)).getAssignVar()+
//                    ")");
            sb.append("\nelse if(("+translatorNew.codeGen.translateExpr(varExpression.getAssignVar())+"=="+translatorNew.codeGen.translateExpr(((PLCVariable)caseTempList.get(0)).getAssignVar())+
                    ")");

            for(int t =1; t<ctx.case_selection(i).case_list().case_list_elem().size(); t++){
                sb.append("\n||("+translatorNew.codeGen.translateExpr(varExpression.getAssignVar())+"=="+ translatorNew.codeGen.translateExpr(ctx.case_selection(i).case_list().case_list_elem(t).getText())+")");
            }

//            System.out.println("){");
            sb.append("\n){");
            result = translatorNew.visit(ctx.case_selection(i).stmt_list());
            sb.append(result);

//            System.out.println("}");
            sb.append("\n}");
        }

        //***********************************************翻译else语句****************************************************
        if(ctx.else_stmt() != null){
            result = translatorNew.visit(ctx.else_stmt());
            sb.append(result);
        }else{
//            System.out.println("else{}");
            sb.append("\nelse{}");
        }
        return sb.toString();
    }
}
