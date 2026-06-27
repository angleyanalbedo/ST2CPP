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
        if (ctx.enum_value() != null) return ctx.enum_value().getText();
        if (ctx.ref_value() != null) return ctx.ref_value().getText();
        return ctx.getChild(0).getText();
    }
}
