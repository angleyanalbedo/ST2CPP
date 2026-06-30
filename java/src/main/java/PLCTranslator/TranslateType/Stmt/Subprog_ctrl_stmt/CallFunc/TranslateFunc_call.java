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
            sb.append("\n\t\t").append(fbInstanceName).append("(").append(paramStr).append(").update(gvl, io, dt);");
        }else if(firstSym instanceof PLCVariable funcVar){
            // 语义检查阶段 CheckFuncCall 返回的 PLCVariable
            // assignVar 格式: *FUNC_NAME(&PARAM1, &PARAM2, )
            // 提取函数名
            String assignVarStr = funcVar.getAssignVar();
            String funcName = extractFuncName(assignVarStr);

            // 遍历 AST param_assign，通过 visitor 翻译每个参数
            // 表达式结果直接内联进函数调用，无需临时变量
            StringBuilder args = new StringBuilder();
            for (PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()) {
                if(param_assignContext instanceof PLCSTPARSERParser.InputParamContext ip){
                    String paramExpr = translatorNew.visit(ip.expression());
                    if (args.length() > 0) args.append(", ");
                    args.append(paramExpr);
                }else{
                    if (args.length() > 0) args.append(", ");
                    args.append(param_assignContext.getText());
                }
            }
            sb.append(funcName).append("(").append(args).append(")");
        }else if(firstSym instanceof PLCBaseFUNDeclSymbol funDecl){
            String funcName = funDecl.getStdFunction();
            StringBuilder args = new StringBuilder();
            for(PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()){
                if(param_assignContext instanceof PLCSTPARSERParser.InputParamContext ip){
                    String paramExpr = translatorNew.visit(ip.expression());
                    if (args.length() > 0) args.append(", ");
                    args.append(paramExpr);
                }else{
                    if (args.length() > 0) args.append(", ");
                    args.append(param_assignContext.getText());
                }
            }
            sb.append(funcName).append("(").append(args).append(")");
        }
        return sb.toString();
    }

    private String extractFuncName(String assignVar) {
        // assignVar 格式: *FUNC_NAME(&PARAM1, &PARAM2, )
        // 或: *FUNC_NAME(&PARAM1, )
        String cleaned = assignVar;
        if (cleaned.startsWith("*")) cleaned = cleaned.substring(1);
        int parenIdx = cleaned.indexOf('(');
        if (parenIdx > 0) {
            return cleaned.substring(0, parenIdx);
        }
        return cleaned;
    }

}
