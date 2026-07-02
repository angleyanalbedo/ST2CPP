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
                String varExpr = PLCVariable.stripParens(assignVar);
                if (translatorNew.gvlCtx.typeMap.containsKey(varExpr)) {
                    varExpr = "gv." + translatorNew.gvlCtx.getMangledName(varExpr);
                }
                sb.append("\n\t\tprintf(\"%d\", (int)(").append(varExpr).append("));");
            }
        }
        sb.append("\n\t\tprintf(\"\\n\");");
        return sb.toString();
    }
}
