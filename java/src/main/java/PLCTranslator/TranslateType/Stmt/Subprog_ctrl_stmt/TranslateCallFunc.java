package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

public class TranslateCallFunc {
    public ArrayList<String> translateNode(PLCSTPARSERParser.CallFuncContext ctx, PLCTranslatorNew translatorNew){
        if(ctx.getChild(0) instanceof PLCSTPARSERParser.Func_callContext childCtx){
            PLCVariable funcSymbol = (PLCVariable) PLCTranslatorNew.properties.get(childCtx).get(0);

            for (PLCSTPARSERParser.Param_assignContext param_assignContext : childCtx.param_assign()) {
                PLCVariable plcVariable =(PLCVariable) PLCTranslatorNew.properties.get(param_assignContext).get(0);
                writeTarget("\n\t\tauto "+plcVariable.getRuntimeName()+"="+plcVariable.getAssignVar()+";");

            }
            String var = funcSymbol.getAssignVar().substring(1);
            writeTarget("\n\t\t" + var + ";");
        }else{
            translatorNew.visit(ctx.getChild(0));
        }


        return null;
    }
}
