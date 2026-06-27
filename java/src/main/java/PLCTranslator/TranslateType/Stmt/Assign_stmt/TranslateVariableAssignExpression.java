package PLCTranslator.TranslateType.Stmt.Assign_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateVariableAssignExpression {
    public String translateNode(PLCSTPARSERParser.VariableAssignExpressionContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        PLCVariable varSymbol = PLCTranslatorNew.getVariable(ctx, "assign target");

        if(varSymbol.getSort() != PLCModifierEnum.Sort.FC) {
            String assignVar = varSymbol.getAssignVar();
            if (assignVar != null && assignVar.contains("(")) {
                // 遍历参数符号，为每个参数生成临时变量声明
                java.util.List<PLCVariable> paramSymbols = getParamSymbols(ctx);
                for (PLCVariable paramVar : paramSymbols) {
                    String paramAssignVar = paramVar.getAssignVar();
                    if (paramAssignVar != null && !paramAssignVar.isEmpty()) {
                        String typeName = paramVar.getRuntimeTypeName();
                        if (typeName == null || typeName.isEmpty()) {
                            typeName = "INT";
                        }
                        typeName = translatorNew.gvlCtx.toNativeType(typeName);
                        String translatedAssignVar = PLCTranslatorNew.gvlCtx.translateExpr(paramAssignVar);
                        sb.append("\n\t\t").append(typeName).append(" ")
                          .append(paramVar.getRuntimeName()).append("=")
                          .append(translatedAssignVar).append(";");
                    }
                }
            }

            String translated = PLCTranslatorNew.gvlCtx.translateExpr(assignVar);
            sb.append("\n\t\t").append(PLCTranslatorNew.gvlCtx.writeExpr(varSymbol.getName(), translated)).append(";");
        }else{
            String translated = PLCTranslatorNew.gvlCtx.translateExpr(varSymbol.getAssignVar());
            sb.append("\n\t\treturn ").append(translated).append(";");
        }
        return sb.toString();
    }

    /**
     * 从赋值表达式右侧提取参数符号列表
     */
    private java.util.List<PLCVariable> getParamSymbols(PLCSTPARSERParser.VariableAssignExpressionContext ctx) {
        java.util.List<PLCVariable> result = new java.util.ArrayList<>();
        collectParams(ctx.expression(), result);
        return result;
    }

    private void collectParams(PLCSTPARSERParser.ExpressionContext expr, java.util.List<PLCVariable> result) {
        if (expr == null) return;
        // expression -> xor_expr -> and_expr -> compare_expr -> equ_expr -> add_expr -> term -> power_expr -> unary_expr -> primary_expr
        PLCSTPARSERParser.Primary_exprContext primary = findPrimaryExpr(expr);
        if (primary != null && primary.func_call() != null) {
            PLCSTPARSERParser.Func_callContext fc = primary.func_call();
            for (PLCSTPARSERParser.Param_assignContext p : fc.param_assign()) {
                try {
                    PLCVariable pv = PLCTranslatorNew.getVariable(p, "function parameter");
                    if (pv != null) result.add(pv);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 从 ExpressionContext 逐层向下找到 Primary_exprContext
     */
    private PLCSTPARSERParser.Primary_exprContext findPrimaryExpr(PLCSTPARSERParser.ExpressionContext expr) {
        if (expr == null) return null;
        PLCSTPARSERParser.Xor_exprContext x = expr.xor_expr(0);
        if (x == null) return null;
        PLCSTPARSERParser.And_exprContext a = x.and_expr(0);
        if (a == null) return null;
        PLCSTPARSERParser.Compare_exprContext c = a.compare_expr(0);
        if (c == null) return null;
        PLCSTPARSERParser.Equ_exprContext e = c.equ_expr(0);
        if (e == null) return null;
        PLCSTPARSERParser.Add_exprContext ad = e.add_expr(0);
        if (ad == null) return null;
        PLCSTPARSERParser.TermContext t = ad.term(0);
        if (t == null) return null;
        PLCSTPARSERParser.Power_exprContext p = t.power_expr(0);
        if (p == null) return null;
        PLCSTPARSERParser.Unary_exprContext u = p.unary_expr(0);
        if (u == null) return null;
        return u.primary_expr();
    }
}
