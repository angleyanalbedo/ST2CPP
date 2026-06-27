package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateVariable_access {
    public String translateNode(PLCSTPARSERParser.Variable_accessContext ctx, PLCTranslatorNew t) {
        String varName = ctx.variable().getText();
        String cleanName = varName.startsWith("*") ? varName.substring(1) : varName;

        // 枚举值模式：TYPE#VALUE → TYPE::VALUE
        if (cleanName.contains("#")) {
            int hashIdx = cleanName.indexOf('#');
            return cleanName.substring(0, hashIdx) + "::" + cleanName.substring(hashIdx + 1);
        }

        if (t.gvlCtx.ioVarMap.containsKey(cleanName)) {
            String ioRead = t.gvlCtx.emitIORead(cleanName);
            if (ioRead != null) return ioRead;
        }

        String type = t.gvlCtx.typeMap.get(cleanName);
        Integer offset = t.gvlCtx.offsetMap.get(cleanName);
        if (type != null && offset != null && !t.gvlCtx.shadowedGvlVars.contains(cleanName)) {
            // cyclic 内直接使用变量名（已通过 prologue 加载为局部变量）
            if (t.inCyclic) {
                return cleanName;
            }
            return "gvl.read<" + type + ">(" + offset + ")";
        }

        return cleanName;
    }
}
