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

        sb.append("\nif(("+translatorNew.gvlCtx.translateExpr(varExpression.getAssignVar())+"=="+translatorNew.gvlCtx.translateExpr(((PLCVariable)caseList.get(0)).getAssignVar())+
                ")");

        for(int t =1; t<ctx.case_selection(0).case_list().case_list_elem().size(); t++){
            sb.append("\n||("+translatorNew.gvlCtx.translateExpr(varExpression.getAssignVar())+"=="
                    + translatorNew.gvlCtx.translateExpr(ctx.case_selection(0).case_list().case_list_elem(t).getText())+")");
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
            sb.append("\nelse if(("+translatorNew.gvlCtx.translateExpr(varExpression.getAssignVar())+"=="+translatorNew.gvlCtx.translateExpr(((PLCVariable)caseTempList.get(0)).getAssignVar())+
                    ")");

            for(int t =1; t<ctx.case_selection(i).case_list().case_list_elem().size(); t++){
                sb.append("\n||("+translatorNew.gvlCtx.translateExpr(varExpression.getAssignVar())+"=="+ translatorNew.gvlCtx.translateExpr(ctx.case_selection(i).case_list().case_list_elem(t).getText())+")");
            }

            sb.append("\n){");
            result = translatorNew.visit(ctx.case_selection(i).stmt_list());
            sb.append(result);

            // If this is the last case_selection AND there's an else_stmt following,
            // don't close the body — emitElse() will provide the closing brace.
            // Otherwise close it now.
            if(ctx.else_stmt() == null || i < ctx.case_selection().size() - 1){
                sb.append("\n}");
            }
        }

        //***********************************************翻译else语句****************************************************
        if(ctx.else_stmt() != null){
            result = translatorNew.visit(ctx.else_stmt());
            sb.append(result);
            sb.append("\n}"); // close the else body
        }else{
            sb.append("\nelse{}");
        }
        return sb.toString();
    }
}
