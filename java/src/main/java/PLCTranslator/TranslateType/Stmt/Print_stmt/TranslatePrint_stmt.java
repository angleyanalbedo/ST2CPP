package PLCTranslator.TranslateType.Stmt.Print_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslatePrint_stmt {
    public String translateNode(PLCSTPARSERParser.Print_stmtContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();
        for (int t = 0; t < ctx.print_stmt_element().size(); t++) {
            PLCSTPARSERParser.Print_stmt_elementContext elemCtx = ctx.print_stmt_element(t);
            ArrayList<PLCSymbol> syms = PLCTranslatorNew.properties.get(elemCtx);
            if (syms == null || syms.isEmpty()) continue;
            PLCVariable elem = (PLCVariable) syms.get(0);
            String assignVar = elem.getAssignVar();
            if (assignVar == null || assignVar.isEmpty()) continue;
            boolean isString = (elem.getSort() == PLCModifierEnum.Sort.STRING);

            if (isString) {
                sb.append("\n\t\tprintf(").append(assignVar).append(");");
            } else {
                // identifier 变量：查 offsetMap 决定输出 gvl.read 或直接用变量名
                String varExpr = stripParens(assignVar);
                String type = translatorNew.gvlCtx.typeMap.get(varExpr);
                Integer offset = translatorNew.gvlCtx.offsetMap.get(varExpr);
                if (type != null && offset != null) {
                    varExpr = "gvl.read<" + type + ">(" + offset + ")";
                }
                sb.append("\n\t\tprintf(\"%d\", (int)(").append(varExpr).append("));");
            }
        }
        sb.append("\n\t\tprintf(\"\\n\");");
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
