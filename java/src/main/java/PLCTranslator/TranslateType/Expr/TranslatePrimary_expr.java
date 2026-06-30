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
        if (ctx.constant() != null) return convertSTNumeric(ctx.constant().getText());
        if (ctx.enum_value() != null) {
            return translateEnumValue(ctx.enum_value(), t);
        }
        if (ctx.ref_value() != null) return ctx.ref_value().getText();

        return ctx.getChild(0).getText();
    }

    /** 将 ST 数值前缀转为 C++ 格式：16#→0x, 2#→0b, 8#→0 */
    public static String convertSTNumeric(String s) {
        if (s == null) return null;
        String result = s;
        // 先处理带类型前缀的字面量（如 DINT#16#0007），去掉类型前缀
        if (result.matches("^[A-Za-z_]+#.*")) {
            int hash = result.indexOf('#');
            String prefix = result.substring(0, hash);
            String rest = result.substring(hash + 1);
            // 如果去掉类型前缀后仍然是 ST 数值格式，继续转换
            if (rest.startsWith("16#")) {
                result = "0x" + rest.substring(3);
            } else if (rest.startsWith("2#")) {
                result = "0b" + rest.substring(2);
            } else if (rest.startsWith("8#")) {
                result = "0" + rest.substring(2);
            } else {
                result = rest;
            }
        } else if (result.startsWith("16#")) {
            result = "0x" + result.substring(3);
        } else if (result.startsWith("2#")) {
            result = "0b" + result.substring(2);
        } else if (result.startsWith("8#")) {
            result = "0" + result.substring(2);
        }
        return result;
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
