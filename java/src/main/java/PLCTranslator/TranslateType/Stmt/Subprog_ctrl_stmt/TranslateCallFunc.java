package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCFBCallSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

public class TranslateCallFunc {
    public String translateNode(PLCSTPARSERParser.CallFuncContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        if(ctx.getChild(0) instanceof PLCSTPARSERParser.Func_callContext childCtx){
            PLCSymbol firstSym = PLCTranslatorNew.getSymbol(childCtx, "call function");
            if(firstSym instanceof PLCFBCallSymbol fbCallSym){
                // FB 调用
                String fbInstanceName = fbCallSym.getFbInstanceName();
                String fbTypeName = translatorNew.gvlCtx.getVarType(fbInstanceName);
                if(fbTypeName == null) fbTypeName = fbInstanceName;
                List<String> paramNames = new ArrayList<>();
                List<String> paramValues = new ArrayList<>();
                // 追踪输出参数：paramName → 目标变量名
                java.util.LinkedHashMap<String, String> outputParams = new java.util.LinkedHashMap<>();
                for(PLCSTPARSERParser.Param_assignContext param_assignContext : childCtx.param_assign()){
                    PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "call parameter");
                    paramNames.add(plcVariable.getName());
                    if (param_assignContext instanceof PLCSTPARSERParser.InputParamContext ip) {
                        paramValues.add(translatorNew.visit(ip.expression()));
                    } else if (param_assignContext instanceof PLCSTPARSERParser.OutParamContext op) {
                        // 输出参数：值用 0 占位（稍后读回），记录目标变量名
                        paramValues.add("0");
                        outputParams.put(plcVariable.getName(), op.variable().getText());
                    } else {
                        paramValues.add(param_assignContext.getText());
                    }
                }
                if(fbCallSym.isArrayElement()){
                    sb.append(emitFBArrayCall(fbCallSym, fbTypeName, paramNames, paramValues, translatorNew));
                } else {
                    sb.append(emitFBCall(fbInstanceName, fbTypeName, paramNames, paramValues, outputParams, translatorNew.gvlCtx));
                }
            }else if(firstSym instanceof PLCVariable funcVar){
                // 检查 AST 是否为 Instance.Method() 模式
                PLCSTPARSERParser.Func_accessContext fa = childCtx.func_access();
                if (fa != null && !fa.scope_name().isEmpty()) {
                    String instanceName = fa.scope_name(0).getText();
                    String methodName = fa.func_name().getText();
                    StringBuilder args = new StringBuilder();
                    for (PLCSTPARSERParser.Param_assignContext p : childCtx.param_assign()) {
                        if (p instanceof PLCSTPARSERParser.InputParamContext ip) {
                            if (args.length() > 0) args.append(", ");
                            args.append(translatorNew.visit(ip.expression()));
                        } else {
                            if (args.length() > 0) args.append(", ");
                            args.append(p.getText());
                        }
                    }
                    sb.append("\n\t\t").append(instanceName).append(".").append(methodName).append("(").append(args).append(");");
                } else {
                    String funcName = funcVar.extractFuncName();
                    StringBuilder args = new StringBuilder();
                    for (PLCSTPARSERParser.Param_assignContext param_assignContext : childCtx.param_assign()) {
                        if (param_assignContext instanceof PLCSTPARSERParser.InputParamContext ip) {
                            String paramExpr = translatorNew.visit(ip.expression());
                            if (args.length() > 0) args.append(", ");
                            args.append(paramExpr);
                        } else {
                            if (args.length() > 0) args.append(", ");
                            args.append(param_assignContext.getText());
                        }
                    }
                    sb.append("\n\t\t").append(funcName).append("(").append(args).append(");");
                }
            }
        }else{
            String result = translatorNew.visit(ctx.getChild(0));
            for (String decl : translatorNew.pendingDecls) {
                sb.append(decl);
            }
            translatorNew.pendingDecls.clear();
            if (result != null) {
                sb.append(result);
            }
        }
        return sb.toString();
    }

    public static String emitFBCall(String fbInstanceName, String fbTypeName,
                                     List<String> paramNames, List<String> paramValues,
                                     java.util.LinkedHashMap<String, String> outputParams,
                                     GvlContext gvlCtx) {
        StringBuilder sb = new StringBuilder();
        String fbMangled = gvlCtx.getMangledName(fbInstanceName);
        // 写入输入参数 → 直接结构体成员赋值
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            String paramValue = paramValues.get(i);
            if (outputParams.containsKey(paramName)) continue;
            sb.append("\n\t\tgv.").append(fbMangled).append(".").append(paramName)
              .append(" = ").append(paramValue).append(";");
        }
        // 调用 update — 直接成员函数调用
        sb.append("\n\t\tgv.").append(fbMangled).append(".update(gvl, io, dt);");
        // 读回输出参数 — 直接成员读取
        for (java.util.Map.Entry<String, String> entry : outputParams.entrySet()) {
            String paramName = entry.getKey();
            String targetVar = entry.getValue();
            String targetBase = targetVar.replaceAll("[\\[.].*", "");
            if (gvlCtx.typeMap.containsKey(targetBase)) {
                if (targetVar.equals(targetBase)) {
                    sb.append("\n\t\tgv.").append(gvlCtx.getMangledName(targetBase))
                      .append(" = gv.").append(fbMangled).append(".").append(paramName).append(";");
                } else {
                    sb.append("\n\t\tgv.").append(gvlCtx.getMangledName(targetBase))
                      .append(targetVar.substring(targetBase.length()))
                      .append(" = gv.").append(fbMangled).append(".").append(paramName).append(";");
                }
            } else {
                sb.append("\n\t\t").append(targetVar).append(" = gv.").append(fbMangled)
                  .append(".").append(paramName).append(";");
            }
        }
        return sb.toString();
    }

    private String emitFBArrayCall(PLCFBCallSymbol fbCallSym, String fbTypeName,
                                    List<String> paramNames, List<String> paramValues,
                                    PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();
        GvlContext gvlCtx = translatorNew.gvlCtx;
        String arrayName = fbCallSym.getFbInstanceName();
        String indexExpr = fbCallSym.getArrayIndexExpr();
        String arrMangled = gvlCtx.getMangledName(arrayName);

        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            String paramValue = paramValues.get(i);
            sb.append("\n\t\tgv.").append(arrMangled).append("[").append(indexExpr)
              .append("].").append(paramName).append(" = ").append(paramValue).append(";");
        }
        sb.append("\n\t\tgv.").append(arrMangled).append("[").append(indexExpr)
          .append("].update(gvl, io, dt);");
        return sb.toString();
    }
}
