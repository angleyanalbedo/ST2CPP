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
                if(fbCallSym.isArrayElement()){
                    sb.append(emitFBArrayCall(fbCallSym, fbTypeName, paramNames, paramValues, translatorNew));
                } else {
                    sb.append(emitFBCall(fbInstanceName, fbTypeName, paramNames, paramValues, translatorNew.gvlCtx));
                }
            }else if(firstSym instanceof PLCVariable funcVar){
                // 普通函数调用（独立语句）
                // assignVar 格式: *FUNC_NAME(&PARAM1, )
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

    /**
     * 数组元素 FB 调用：Fbs[I](X := value)
     * 计算 baseOffset + index * elementSize
     */
    private String emitFBArrayCall(PLCFBCallSymbol fbCallSym, String fbTypeName,
                                    List<String> paramNames, List<String> paramValues,
                                    PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();
        GvlContext gvlCtx = translatorNew.gvlCtx;
        String arrayName = fbCallSym.getFbInstanceName();
        String indexExpr = fbCallSym.getArrayIndexExpr();

        // 获取数组基地址
        Integer arrayBase = gvlCtx.offsetMap.get(arrayName);
        if(arrayBase == null){
            // 尝试解析为结构体成员（如 ROBOT_CTRL.Srv）
            arrayBase = gvlCtx.resolveStructMemberOffset(arrayName);
        }
        if(arrayBase == null){
            // 非 GVL 数组（如 FB 体内的成员数组 Srv[I]）：直接生成成员访问
            // 提取元素类型名
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

        // 提取元素类型名（从 "ARRAY[3] OF MY_FB" → "MY_FB"）
        String elemTypeName = fbTypeName;
        if(fbTypeName != null && fbTypeName.startsWith("ARRAY[")){
            int ofIdx = fbTypeName.indexOf(" OF ");
            if(ofIdx >= 0){
                elemTypeName = fbTypeName.substring(ofIdx + 4).trim();
            }
        }

        // 计算元素大小
        int elemSize = gvlCtx.getTypeSize(elemTypeName);
        if(elemSize == 0) elemSize = 64; // fallback

        // 计算参数偏移（数组元素内部）
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
        // 调用 update — 使用元素类型
        sb.append("\n\t\tgvl.ptr<").append(elemTypeName).append(">(")
          .append(arrayBase).append(" + (").append(indexExpr).append(") * ").append(elemSize)
          .append(")->update(gvl, io, dt);");
        return sb.toString();
    }
}
