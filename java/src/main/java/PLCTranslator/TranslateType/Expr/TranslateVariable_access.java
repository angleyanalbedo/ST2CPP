package PLCTranslator.TranslateType.Expr;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateVariable_access {
    public String translateNode(PLCSTPARSERParser.Variable_accessContext ctx, PLCTranslatorNew t) {
        // 优先使用语义分析结果（已转换多维下标 [I,0]→[I][0] 等）
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

        if (t.inCyclic) {
            if (cleanName.startsWith("(") && cleanName.endsWith(")")) {
                cleanName = cleanName.substring(1, cleanName.length() - 1);
            }
            return cleanName;
        }

        if (t.gvlCtx.isIOVariable(cleanName)) {
            String ioRead = t.gvlCtx.emitIORead(cleanName);
            if (ioRead != null) return ioRead;
        }

        String type = t.gvlCtx.typeMap.get(cleanName);
        Integer offset = t.gvlCtx.offsetMap.get(cleanName);
        if (type != null && offset != null && !t.gvlCtx.shadowedGvlVars.contains(cleanName)) {
            return "gvl.read<" + type + ">(" + offset + ")";
        }

        return cleanName;
    }
}
