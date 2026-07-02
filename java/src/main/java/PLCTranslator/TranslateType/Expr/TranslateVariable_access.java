package PLCTranslator.TranslateType.Expr;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateVariable_access {
    public String translateNode(PLCSTPARSERParser.Variable_accessContext ctx, PLCTranslatorNew t) {
        String varName = ctx.variable().getText();
        try {
            PLCVariable sym = PLCTranslatorNew.getVariable(ctx, "var access");
            String n = sym.getName();
            if (n != null && !n.isEmpty()) varName = n;
        } catch (RuntimeException ignored) {
        }
        String cleanName = varName.startsWith("*") ? varName.substring(1) : varName;

        if (cleanName.contains("#")) {
            int hashIdx = cleanName.indexOf('#');
            return cleanName.substring(0, hashIdx) + "::" + cleanName.substring(hashIdx + 1);
        }

        // I/O 映射变量
        if (t.gvlCtx.isIOVariable(cleanName)) {
            String ioRead = t.gvlCtx.emitIORead(cleanName);
            if (ioRead != null) return ioRead;
        }

        // GVL 变量 → 通过 layout 直接访问
        // Handle array subscript: extract base name before '['
        String normalizedName = cleanName;
        while (normalizedName.startsWith("(") && normalizedName.endsWith(")")) {
            normalizedName = normalizedName.substring(1, normalizedName.length() - 1);
        }
        int bracketIdx = normalizedName.indexOf('[');
        String lookupName = bracketIdx >= 0 ? normalizedName.substring(0, bracketIdx) : normalizedName;

        if (t.gvlCtx.typeMap.containsKey(lookupName) && !t.gvlCtx.shadowedGvlVars.contains(lookupName)) {
            String mangled = "gv." + t.gvlCtx.getMangledName(lookupName);
            if (bracketIdx >= 0) {
                String indexPart = t.gvlCtx.translateExpr(normalizedName.substring(bracketIdx));
                return mangled + indexPart;
            }
            return mangled;
        }

        return cleanName;
    }
}
