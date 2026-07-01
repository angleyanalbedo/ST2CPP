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
                for(PLCSTPARSERParser.Param_assignContext param_assignContext : childCtx.param_assign()){
                    PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "call parameter");
                    paramNames.add(plcVariable.getName());
                    if (param_assignContext instanceof PLCSTPARSERParser.InputParamContext ip) {
                        paramValues.add(translatorNew.visit(ip.expression()));
                    } else {
                        paramValues.add(param_assignContext.getText());
                    }
                }
                if(fbCallSym.isArrayElement()){
                    sb.append(emitFBArrayCall(fbCallSym, fbTypeName, paramNames, paramValues, translatorNew));
                } else {
                    sb.append(emitFBCall(fbInstanceName, fbTypeName, paramNames, paramValues, translatorNew.gvlCtx));
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
                    String funcName = extractFuncName(funcVar.getAssignVar());
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
              .append(fbBase).append(")->update(gvl, io, dt);");
        } else {
            sb.append("\n\t\t").append(fbInstanceName).append(".update(gvl, io, dt);");
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

        Integer arrayBase = gvlCtx.offsetMap.get(arrayName);
        if(arrayBase == null){
            arrayBase = gvlCtx.resolveStructMemberOffset(arrayName);
        }
        if(arrayBase == null){
            String elemTypeName = fbTypeName;
            if(fbTypeName != null && fbTypeName.startsWith("ARRAY[")){
                int ofIdx = fbTypeName.indexOf(" OF ");
                if(ofIdx >= 0) elemTypeName = fbTypeName.substring(ofIdx + 4).trim();
            }
            for (int i = 0; i < paramNames.size(); i++) {
                String paramName = paramNames.get(i);
                String paramValue = paramValues.get(i);
                sb.append("\n\t\t").append(arrayName).append("[").append(indexExpr)
                  .append("].").append(paramName).append(" = ").append(paramValue).append(";");
            }
            sb.append("\n\t\t").append(arrayName).append("[").append(indexExpr)
              .append("].update(gvl, io, dt);");
            return sb.toString();
        }

        String elemTypeName = fbTypeName;
        if(fbTypeName != null && fbTypeName.startsWith("ARRAY[")){
            int ofIdx = fbTypeName.indexOf(" OF ");
            if(ofIdx >= 0) elemTypeName = fbTypeName.substring(ofIdx + 4).trim();
        }

        int elemSize = gvlCtx.getTypeSize(elemTypeName);
        if(elemSize == 0) elemSize = 64;

        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            String paramValue = paramValues.get(i);
            Integer fieldOff = gvlCtx.getStructFieldOffset(elemTypeName, paramName);
            String type = gvlCtx.getStructFieldType(elemTypeName, paramName);
            if(fieldOff != null && type != null){
                sb.append("\n\t\tgvl.write<").append(type).append(">(")
                  .append(arrayBase).append(" + (").append(indexExpr).append(") * ").append(elemSize)
                  .append(" + ").append(fieldOff).append(", ").append(paramValue).append(");");
            }
        }
        sb.append("\n\t\tgvl.ptr<").append(elemTypeName).append(">(")
          .append(arrayBase).append(" + (").append(indexExpr).append(") * ").append(elemSize)
          .append(")->update(gvl, io, dt);");
        return sb.toString();
    }
}
