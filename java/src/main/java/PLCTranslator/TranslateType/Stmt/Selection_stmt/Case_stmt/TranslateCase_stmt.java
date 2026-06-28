package PLCTranslator.TranslateType.Stmt.Selection_stmt.Case_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSubrangeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateCase_stmt {
    public String translateNode(PLCSTPARSERParser.Case_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        String caseExpr = translatorNew.visit(ctx.expression());

        sb.append("\n\tswitch(").append(caseExpr).append("){");

        for(int i = 0; i < ctx.case_selection().size(); i++){
            ArrayList<PLCSymbol> caseList = PLCTranslatorNew.properties.get(ctx.case_selection(i).case_list());

            for(PLCSymbol elem : caseList){
                if(elem instanceof PLCSubrangeDeclSymbol){
                    PLCSubrangeDeclSymbol range = (PLCSubrangeDeclSymbol) elem;
                    int lower = Integer.parseInt(range.getLowerLimit().trim());
                    int upper = Integer.parseInt(range.getUpperLimit().trim());
                    for(int v = lower; v <= upper; v++){
                        sb.append("\n\t\tcase ").append(v).append(":");
                    }
                } else {
                    PLCVariable var = (PLCVariable) elem;
                    sb.append("\n\t\tcase ").append(stripParens(var.getAssignVar())).append(":");
                }
            }

            String result = translatorNew.visit(ctx.case_selection(i).stmt_list());
            sb.append(indentBody(result));
            sb.append("\n\t\tbreak;");
        }

        if(ctx.else_stmt() != null){
            sb.append("\n\t\tdefault:");
            String result = translatorNew.visit(ctx.else_stmt().stmt_list());
            sb.append(indentBody(result));
            sb.append("\n\t\tbreak;");
        }

        sb.append("\n\t}");
        return sb.toString();
    }

    private String indentBody(String body) {
        if (body == null) return "";
        StringBuilder out = new StringBuilder();
        String[] lines = body.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                out.append("\t").append(lines[i]);
            } else {
                out.append(lines[i]);
            }
            if (i < lines.length - 1) {
                out.append("\n");
            }
        }
        return out.toString();
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
