package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateAnd_expr {
    public String translateNode(PLCSTPARSERParser.And_exprContext ctx, PLCTranslatorNew t) {
        if (ctx.compare_expr().size() == 1) {
            return t.visit(ctx.getChild(0));
        }
        // 检测操作数是否含比较运算符，决定逻辑/位运算
        String firstChild = t.visit(ctx.getChild(0));
        boolean isLogical = firstChild.contains("<") || firstChild.contains(">")
                || firstChild.contains("==") || firstChild.contains("!=");
        String op = isLogical ? " && " : " & ";

        StringBuilder sb = new StringBuilder();
        sb.append(t.visit(ctx.getChild(0)));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            sb.append(op).append(t.visit(ctx.getChild(i + 1)));
        }
        return sb.toString();
    }
}
