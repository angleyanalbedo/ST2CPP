package PLCTranslator.TranslateType.Stmt.Selection_stmt.Case_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateCase_stmt {
    public String translateNode(PLCSTPARSERParser.Case_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        // 通过 visitor 翻译 case 表达式（替代 translateExpr(assignVar)）
        String caseExpr = translatorNew.visit(ctx.expression());

        ArrayList<PLCSymbol> caseList = PLCTranslatorNew.properties.get(ctx.case_selection(0).case_list());

        sb.append("\nif((").append(caseExpr).append("==").append(translatorNew.gvlCtx.translateExpr(((PLCVariable)caseList.get(0)).getAssignVar())).append(")");

        for(int t =1; t<ctx.case_selection(0).case_list().case_list_elem().size(); t++){
            sb.append("\n||(").append(caseExpr).append("==")
                    .append(translatorNew.gvlCtx.translateExpr(ctx.case_selection(0).case_list().case_list_elem(t).getText())).append(")");
        }

        sb.append("){");
        String result = translatorNew.visit(ctx.case_selection(0).stmt_list());
        sb.append(result);
        sb.append("}");

        for(int i = 1; i<ctx.case_selection().size(); i++){
            ArrayList<PLCSymbol> caseTempList = PLCTranslatorNew.properties.get(ctx.case_selection(i).case_list());
            sb.append("\nelse if((").append(caseExpr).append("==").append(translatorNew.gvlCtx.translateExpr(((PLCVariable)caseTempList.get(0)).getAssignVar())).append(")");

            for(int t =1; t<ctx.case_selection(i).case_list().case_list_elem().size(); t++){
                sb.append("\n||(").append(caseExpr).append("==").append(translatorNew.gvlCtx.translateExpr(ctx.case_selection(i).case_list().case_list_elem(t).getText())).append(")");
            }

            sb.append("\n){");
            result = translatorNew.visit(ctx.case_selection(i).stmt_list());
            sb.append(result);

            if(ctx.else_stmt() == null || i < ctx.case_selection().size() - 1){
                sb.append("\n}");
            }
        }

        if(ctx.else_stmt() != null){
            result = translatorNew.visit(ctx.else_stmt());
            sb.append(result);
            sb.append("\n}");
        }else{
            sb.append("\nelse{}");
        }
        return sb.toString();
    }
}
