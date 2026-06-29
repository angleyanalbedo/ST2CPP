package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCFBCallSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCSymbolAndScope.PLCSymbols.PLCBaseFUNDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCBaseFUNDeclSymbol;
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
                // FB 调用 — 保持不变
                String fbInstanceName = fbCallSym.getFbInstanceName();
                String fbTypeName = translatorNew.gvlCtx.getVarType(fbInstanceName);
                if(fbTypeName == null) fbTypeName = fbInstanceName;
                List<String> paramNames = new ArrayList<>();
                List<String> paramValues = new ArrayList<>();
                for(PLCSTPARSERParser.Param_assignContext param_assignContext : childCtx.param_assign()){
                    PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "call parameter");
                    paramNames.add(plcVariable.getName());
                    // 通过 visitor 翻译参数值（替代 translateExpr）
                    if (param_assignContext instanceof PLCSTPARSERParser.InputParamContext ip) {
                        paramValues.add(translatorNew.visit(ip.expression()));
                    } else {
                        paramValues.add(param_assignContext.getText());
                    }
                }
                sb.append(emitFBCall(fbInstanceName, fbTypeName, paramNames, paramValues, translatorNew.gvlCtx));
            }else if(firstSym instanceof PLCVariable funcVar){
                // 普通函数调用（独立语句）
                // assignVar 格式: *FUNC_NAME(&PARAM1, )
                String funcName = extractFuncName(funcVar.getAssignVar());
                for (PLCSTPARSERParser.Param_assignContext param_assignContext : childCtx.param_assign()) {
                    PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "call parameter");
                    if (param_assignContext instanceof PLCSTPARSERParser.InputParamContext ip) {
                        String typeName = plcVariable.getRuntimeTypeName();
                        if (typeName == null || typeName.isEmpty()) {
                            typeName = "INT";
                        }
                        typeName = translatorNew.gvlCtx.toNativeType(typeName);
                        // visitor 翻译参数表达式
                        String exprResult = translatorNew.visit(ip.expression());
                        sb.append("\n\t\t").append(typeName).append(" ")
                          .append(plcVariable.getRuntimeName()).append("=").append(exprResult).append(";");
                    }
                }
                sb.append("\n\t\t").append(funcName).append(";");
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

    private String extractFuncName(String assignVar) {
        String cleaned = assignVar;
        if (cleaned.startsWith("*")) cleaned = cleaned.substring(1);
        int parenIdx = cleaned.indexOf('(');
        if (parenIdx > 0) return cleaned.substring(0, parenIdx);
        return cleaned;
    }

    public static String emitFBCall(String fbInstanceName, String fbTypeName,
                                     List<String> paramNames, List<String> paramValues,
                                     GvlContext gvlCtx) {
        StringBuilder sb = new StringBuilder();
        Integer fbBase = gvlCtx.offsetMap.get(fbInstanceName);
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            String paramValue = paramValues.get(i);
            Integer offset = gvlCtx.offsetMap.get(paramName);
            String type = gvlCtx.typeMap.get(paramName);
            if (offset == null && fbBase != null) {
                Integer fieldOff = gvlCtx.getStructFieldOffset(fbTypeName, paramName);
                if (fieldOff != null) {
                    offset = fbBase + fieldOff;
                    type = gvlCtx.getStructFieldType(fbTypeName, paramName);
                }
            }
            if (offset != null && type != null) {
                sb.append("\n\t\tgvl.write<").append(type).append(">(")
                  .append(offset).append(", ").append(paramValue).append(");");
            }
        }
        if (fbBase != null) {
            sb.append("\n\t\tgvl.ptr<").append(fbTypeName).append(">(")
              .append(fbBase).append(")->update(dt);");
        } else {
            sb.append("\n\t\t").append(fbInstanceName).append(".update(dt);");
        }
        return sb.toString();
    }
}
