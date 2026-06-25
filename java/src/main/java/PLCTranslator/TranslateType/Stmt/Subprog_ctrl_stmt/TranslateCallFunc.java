package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.tree.ParseTree;

public class TranslateCallFunc {
    public String translateNode(PLCSTPARSERParser.CallFuncContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        if(ctx.getChild(0) instanceof PLCSTPARSERParser.Func_callContext childCtx){
            PLCVariable funcSymbol = (PLCVariable) PLCTranslatorNew.properties.get(childCtx).get(0);

            for (PLCSTPARSERParser.Param_assignContext param_assignContext : childCtx.param_assign()) {
                PLCVariable plcVariable =(PLCVariable) PLCTranslatorNew.properties.get(param_assignContext).get(0);
                String typeName = plcVariable.getRuntimeTypeName();
                if (typeName == null || typeName.isEmpty()) {
                    typeName = "INT";
                }
                typeName = ((PLCTranslator.FlatCodeGenerator) translatorNew.codeGen).toNativeType(typeName);
                sb.append("\n\t\t" + typeName + " " + plcVariable.getRuntimeName() + "=" + translatorNew.codeGen.translateExpr(plcVariable.getAssignVar()) + ";");

            }
            String var = translatorNew.codeGen.translateExpr(funcSymbol.getAssignVar()).substring(1);
            sb.append("\n\t\t" + var + ";");
        }else{
            String result = translatorNew.visit(ctx.getChild(0));
            sb.append(result);
        }


        return sb.toString();
    }
}
