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
        if (t.gvlCtx.typeMap.containsKey(cleanName) && !t.gvlCtx.shadowedGvlVars.contains(cleanName)) {
            return "gv." + t.gvlCtx.getMangledName(cleanName);
        }

        return cleanName;
    }
}
