package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslatePrimary_expr {
    public String translateNode(PLCSTPARSERParser.Primary_exprContext ctx, PLCTranslatorNew t) {
        if (ctx.expression() != null) {
            return "(" + t.visit(ctx.expression()) + ")";
        }
        if (ctx.variable_access() != null) {
            return new TranslateVariable_access().translateNode(ctx.variable_access(), t);
        }
        if (ctx.func_call() != null) {
            return t.visit(ctx.func_call());
        }
        if (ctx.constant() != null) return ctx.constant().getText();
        if (ctx.enum_value() != null) {
            String valueName = ctx.enum_value().identifier().getText();
            if (t.inCyclic) {
                return valueName;
            }
            if (t.gvlCtx.isIOVariable(valueName)) {
                String ioRead = t.gvlCtx.emitIORead(valueName);
                if (ioRead != null) return ioRead;
            }
            String type = t.gvlCtx.typeMap.get(valueName);
            Integer offset = t.gvlCtx.offsetMap.get(valueName);
            if (type != null && offset != null && !t.gvlCtx.shadowedGvlVars.contains(valueName)) {
                return "gvl.read<" + type + ">(" + offset + ")";
            }
            return translateEnumValue(ctx.enum_value(), t);
        }
        if (ctx.ref_value() != null) return ctx.ref_value().getText();

        return ctx.getChild(0).getText();
    }

    private String translateEnumValue(antlr4.PLCSTPARSERParser.Enum_valueContext ctx, PLCTranslatorNew t) {
        String typeName = ctx.enum_type_name() != null ? ctx.enum_type_name().getText() : null;
        String valueName = ctx.identifier().getText();

        // 枚举值输出为 TypeName::valueName
        if (typeName != null) {
            return typeName + "::" + valueName;
        }
        // 无类型前缀：直接输出值名
        return valueName;
    }
}
