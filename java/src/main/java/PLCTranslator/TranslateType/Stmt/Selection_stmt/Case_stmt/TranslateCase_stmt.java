package PLCTranslator.TranslateType.Stmt.Selection_stmt.Case_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateCase_stmt {
    public String translateNode(PLCSTPARSERParser.Case_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        // 通过 visitor 翻译 case 表达式
        String caseExpr = translatorNew.visit(ctx.expression());

        ArrayList<PLCSymbol> caseList = PLCTranslatorNew.properties.get(ctx.case_selection(0).case_list());

        String caseVal0 = stripParens(((PLCVariable)caseList.get(0)).getAssignVar());
        sb.append("\nif((").append(caseExpr).append("==").append(caseVal0).append(")");

        for(int t = 1; t < ctx.case_selection(0).case_list().case_list_elem().size(); t++){
            String elemVal = ctx.case_selection(0).case_list().case_list_elem(t).getText();
            sb.append("\n||(").append(caseExpr).append("==").append(elemVal).append(")");
        }

        sb.append("){");
        String result = translatorNew.visit(ctx.case_selection(0).stmt_list());
        sb.append(result);
        sb.append("}");

        for(int i = 1; i < ctx.case_selection().size(); i++){
            ArrayList<PLCSymbol> caseTempList = PLCTranslatorNew.properties.get(ctx.case_selection(i).case_list());
            String caseValI = stripParens(((PLCVariable)caseTempList.get(0)).getAssignVar());
            sb.append("\nelse if((").append(caseExpr).append("==").append(caseValI).append(")");

            for(int t = 1; t < ctx.case_selection(i).case_list().case_list_elem().size(); t++){
                String elemVal = ctx.case_selection(i).case_list().case_list_elem(t).getText();
                sb.append("\n||(").append(caseExpr).append("==").append(elemVal).append(")");
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

    private String stripParens(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("(") && s.endsWith(")")) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }
}
