package PLCTranslator.TranslateType.Expr;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateVariable_access {
    public String translateNode(PLCSTPARSERParser.Variable_accessContext ctx, PLCTranslatorNew t) {
        PLCVariable var = PLCTranslatorNew.getVariable(ctx, "variable access");
        String varName = var.getName();
        String cleanName = varName.startsWith("*") ? varName.substring(1) : varName;

        if (t.gvlCtx.ioVarMap.containsKey(cleanName)) {
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
