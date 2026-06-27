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
            ArrayList<PLCSymbol> syms = PLCTranslatorNew.properties.get(ctx.print_stmt_element(t));
            if (syms == null || syms.isEmpty()) continue;
            PLCVariable elem = (PLCVariable) syms.get(0);
            String assignVar = elem.getAssignVar();
            if (assignVar == null || assignVar.isEmpty()) continue;
            boolean isString = (elem.getSort() == PLCModifierEnum.Sort.STRING);
            String translated = isString ? assignVar : translatorNew.gvlCtx.translateExpr(assignVar);
            if (isString) {
                sb.append("\n\t\tprintf(").append(translated).append(");");
            } else {
                sb.append("\n\t\tprintf(\"%d\", (int)(").append(translated).append("));");
            }
        }
        // 新行
        sb.append("\n\t\tprintf(\"\\n\");");
        return sb.toString();
    }
}
