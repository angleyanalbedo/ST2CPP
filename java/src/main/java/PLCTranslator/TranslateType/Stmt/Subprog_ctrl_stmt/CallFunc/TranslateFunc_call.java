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
            // 检查 AST 是否为 Instance.Method() 模式
            PLCSTPARSERParser.Func_accessContext fa = ctx.func_access();
            if (fa != null && !fa.scope_name().isEmpty()) {
                // Instance.Method(args) 模式 — CLASS 成员方法调用
                String instanceName = fa.scope_name(0).getText();
                String methodName = fa.func_name().getText();
                StringBuilder args = new StringBuilder();
                for (PLCSTPARSERParser.Param_assignContext p : ctx.param_assign()) {
                    if (p instanceof PLCSTPARSERParser.InputParamContext ip) {
                        if (args.length() > 0) args.append(", ");
                        args.append(translatorNew.visit(ip.expression()));
                    } else {
                        if (args.length() > 0) args.append(", ");
                        args.append(p.getText());
                    }
                }
                sb.append(instanceName).append(".").append(methodName).append("(").append(args).append(")");
            } else {
                // 普通函数调用
                String funcName = funcVar.extractFuncName();
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
            }
        }else if(firstSym instanceof PLCBaseFUNDeclSymbol funDecl){
            // 优先使用 runtimeTypeName（格式 ClassName.InstanceName.MethodName）
            String runtimeTypeName = funDecl.getRuntimeTypeName();
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
            // 检查是否为 CLASS 方法调用（runtimeTypeName 格式：ClassName.InstanceName.MethodName）
            String classInstanceCall = tryClassMethodCall(runtimeTypeName, args.toString());
            if (classInstanceCall != null) {
                sb.append(classInstanceCall);
            } else {
                sb.append(funcName).append("(").append(args).append(")");
            }
        }
        return sb.toString();
    }

    private String tryClassMethodCall(String runtimeTypeName, String args) {
        if (runtimeTypeName == null) return null;
        String[] parts = runtimeTypeName.split("\\.");
        if (parts.length == 3) {
            String instanceName = parts[1];
            String methodName = parts[2];
            return "\n\t\t" + instanceName + "." + methodName + "(" + args + ");";
        }
        return null;
    }

}
