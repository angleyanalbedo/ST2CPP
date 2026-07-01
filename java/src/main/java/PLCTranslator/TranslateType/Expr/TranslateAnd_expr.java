package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateAnd_expr {
    public String translateNode(PLCSTPARSERParser.And_exprContext ctx, PLCTranslatorNew t) {
        // 检查第一个操作数是否是比较表达式（含 <, >, ==, !=, <=, >=）
        // 比较表达式的结果是 BOOL → 用 &&；否则是整数 → 用 &
        String firstChild = t.visit(ctx.getChild(0));
        boolean hasCompare = firstChild.contains("<") || firstChild.contains(">")
                || firstChild.contains("==") || firstChild.contains("!=");
        return PLCTranslatorNew.translateBinaryChain(ctx, 1, 2, t, hasCompare);
    }
}
