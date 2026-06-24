package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.CallFunc;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译函数调用
 *
 * <funcRuntimeName>::callFunc(<funcField>,<funcPara>*)
 *
 *
 */



public class TranslateFunc_call {
    public String translateNode(PLCSTPARSERParser.Func_callContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        //

        PLCVariable funcSymbol = (PLCVariable) PLCTranslatorNew.properties.get(ctx).get(0);

        for (PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()) {
            PLCVariable plcVariable =(PLCVariable) PLCTranslatorNew.properties.get(param_assignContext).get(0);
            String typeName = plcVariable.getRuntimeTypeName();
            if (typeName == null || typeName.isEmpty()) {
                typeName = "INT"; // 默认类型
            }
            if (translatorNew.codeGen instanceof PLCTranslator.FlatCodeGenerator) {
                typeName = ((PLCTranslator.FlatCodeGenerator) translatorNew.codeGen).toNativeType(typeName);
            }
            sb.append("\n\t\t" + typeName + " " + plcVariable.getRuntimeName() + "=" + translatorNew.codeGen.translateExpr(plcVariable.getAssignVar()) + ";");

        }
        return sb.toString();
    }
}
