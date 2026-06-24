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
            sb.append("\n\t\tauto "+plcVariable.getRuntimeName()+"="+translatorNew.codeGen.translateExpr(plcVariable.getAssignVar())+";");

        }
        return sb.toString();
    }
}
