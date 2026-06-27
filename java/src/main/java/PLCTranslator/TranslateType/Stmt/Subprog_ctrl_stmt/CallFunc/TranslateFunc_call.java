package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.CallFunc;

import PLCSymbolAndScope.PLCSymbols.PLCFBCallSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCBaseFUNDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateFunc_call {
    public String translateNode(PLCSTPARSERParser.Func_callContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        PLCSymbol firstSym = PLCTranslatorNew.getSymbol(ctx, "function call");
        if(firstSym instanceof PLCFBCallSymbol fbCallSym){
            String fbInstanceName = fbCallSym.getFbInstanceName();
            String fbTypeName = translatorNew.gvlCtx.getVarType(fbInstanceName);
            if(fbTypeName == null) fbTypeName = fbInstanceName;
            StringBuilder paramStr = new StringBuilder();
            for(PLCSTPARSERParser.Param_assignContext p : ctx.param_assign()){
                PLCVariable pv = PLCTranslatorNew.getVariable(p, "function parameter");
                if(paramStr.length() > 0) paramStr.append(", ");
                paramStr.append(pv.getName()).append(" := ").append(translatorNew.visit(p));
            }
            sb.append("\n\t\t").append(fbInstanceName).append("(").append(paramStr).append(").update();");
        }else if(firstSym instanceof PLCBaseFUNDeclSymbol funDecl){
            String funcName = funDecl.getStdFunction();
            StringBuilder args = new StringBuilder();
            for(PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()){
                PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "function parameter");
                if(param_assignContext instanceof PLCSTPARSERParser.InputParamContext ip){
                    String exprResult = translatorNew.visit(ip.expression());
                    String paramExpr;
                    if (isSimpleExpression(ip.expression())) {
                        paramExpr = exprResult;
                    } else {
                        String typeName = resolveTypeName(plcVariable, translatorNew);
                        translatorNew.pendingDecls.add("\n\t\t" + typeName + " " + plcVariable.getRuntimeName() + "=" + exprResult + ";");
                        paramExpr = plcVariable.getRuntimeName();
                    }
                    if (args.length() > 0) args.append(", ");
                    args.append(paramExpr);
                }else{
                    if (args.length() > 0) args.append(", ");
                    args.append(param_assignContext.getText());
                }
            }
            sb.append(funcName).append("(").append(args).append(")");
        }else if(firstSym instanceof PLCVariable funcVar){
            // 语义检查阶段 CheckFuncCall 返回的 PLCVariable
            // assignVar 已包含完整函数调用表达式（如 WRAP_ANGLE(VAL)）
            String translated = PLCTranslatorNew.gvlCtx.translateExpr(funcVar.getAssignVar());
            sb.append(translated);
        }
        return sb.toString();
    }

    private String resolveTypeName(PLCVariable plcVariable, PLCTranslatorNew translatorNew) {
        String typeName = plcVariable.getRuntimeTypeName();
        if(typeName == null || typeName.isEmpty() || "INT".equals(typeName)){
            int tid = plcVariable.getTypeId();
            if(tid != 0){
                PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol tdecl =
                    PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable.getTypeByTypeID(tid);
                if(tdecl != null && tdecl.getName() != null){
                    typeName = tdecl.getName();
                }
            }
        }
        if(typeName == null || typeName.isEmpty()){
            typeName = "INT";
        }
        return translatorNew.gvlCtx.toNativeType(typeName);
    }

    private boolean isSimpleExpression(PLCSTPARSERParser.ExpressionContext expr) {
        if (expr == null) return false;
        String text = expr.getText().trim();
        // 纯数字常数
        if (text.matches("^-?\\d+\\.?\\d*$")) return true;
        // 简单变量名（不含运算符、函数调用）
        if (text.matches("^[A-Za-z_][A-Za-z0-9_]*$")) return true;
        return false;
    }
}
