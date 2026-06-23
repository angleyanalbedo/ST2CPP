package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.Invocation;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

/**
 *  翻译类或方法块实例方法函数调用
 */
public class TranslateInvocation1 {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Invocation1Context ctx, PLCTranslatorNew translatorNew){

        PLCVariable instanceSymbol = (PLCVariable) PLCTranslatorNew.properties.get(ctx.invocation1branch()).get(0);

        String methodName = ctx.method_name().getText();

//        System.out.print(instanceSymbol.getRuntimeName()+
//                "::"+methodName+"::callFunc(meth" +methodName);
        for (PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()) {
            PLCVariable paraSymbol = (PLCVariable) PLCTranslatorNew.properties.get(param_assignContext).get(0);

//            System.out.print(","+paraSymbol.getRuntimeName()); //类内函数调用细化修改
        }
//        System.out.print(");");
        return null;
    }
}
