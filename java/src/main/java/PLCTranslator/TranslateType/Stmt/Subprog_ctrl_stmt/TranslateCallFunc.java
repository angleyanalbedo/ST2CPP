package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.ArrayList;

public class TranslateCallFunc {
    public String translateNode(PLCSTPARSERParser.CallFuncContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        if(ctx.getChild(0) instanceof PLCSTPARSERParser.Func_callContext childCtx){
            ArrayList<PLCSymbol> symbols = PLCTranslatorNew.properties.get(childCtx);
            PLCSymbol firstSym = (symbols != null && !symbols.isEmpty()) ? symbols.get(0) : null;

            // 检测 FB 实例调用
            if (firstSym instanceof PLCVariable fbCallVar
                && "fb_instance_call".equals(fbCallVar.getRuntimeTypeName())) {
                FlatCodeGenerator flatGen = (FlatCodeGenerator) translatorNew.codeGen;
                int fbTypeId = fbCallVar.getTypeId();
                PLCTypeDeclSymbol typeSym = PLCTotalSymbolTable.getTypeByTypeID(fbTypeId);
                String fbTypeName = typeSym != null ? typeSym.getName() : "UNKNOWN";
                String fbVarName = fbCallVar.getName();
                Integer baseOffset = flatGen.getOffsetMap().get(fbVarName);

                for (PLCSTPARSERParser.Param_assignContext param_assignContext : childCtx.param_assign()) {
                    PLCVariable plcVar = PLCTranslatorNew.getVariable(param_assignContext, "call parameter");
                    String paramName = plcVar.getName();
                    String paramValue = translatorNew.codeGen.translateExpr(plcVar.getAssignVar());
                    if (baseOffset != null) {
                        Integer fieldOff = flatGen.getStructFieldOffset(fbTypeName, paramName);
                        if (fieldOff != null) {
                            String fieldType = flatGen.toNativeType(plcVar.getRuntimeTypeName());
                            sb.append("\n\t\tgvl.write<").append(fieldType).append(">(")
                              .append(baseOffset + fieldOff).append(", ").append(paramValue).append(");");
                        }
                    }
                }
                if (baseOffset != null) {
                    sb.append("\n\t\tgvl.ptr<").append(fbTypeName).append(">(")
                      .append(baseOffset).append(")->update();");
                }
            } else {
                PLCVariable funcSymbol = PLCTranslatorNew.getVariable(childCtx, "call function");
                for (PLCSTPARSERParser.Param_assignContext param_assignContext : childCtx.param_assign()) {
                    PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "call parameter");
                    String typeName = plcVariable.getRuntimeTypeName();
                    if (typeName == null || typeName.isEmpty()) {
                        typeName = "INT";
                    }
                    typeName = ((FlatCodeGenerator) translatorNew.codeGen).toNativeType(typeName);
                    sb.append("\n\t\t" + typeName + " " + plcVariable.getRuntimeName() + "=" + translatorNew.codeGen.translateExpr(plcVariable.getAssignVar()) + ";");
                }
                String var = translatorNew.codeGen.translateExpr(funcSymbol.getAssignVar()).substring(1);
                sb.append("\n\t\t" + var + ";");
            }
        }else{
            String result = translatorNew.visit(ctx.getChild(0));
            sb.append(result);
        }


        return sb.toString();
    }
}
