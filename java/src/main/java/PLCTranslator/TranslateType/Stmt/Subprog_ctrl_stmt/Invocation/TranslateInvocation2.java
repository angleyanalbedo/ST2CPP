package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.Invocation;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

/**
 * 翻译功能块和类对象调用
 */
public class TranslateInvocation2 {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Invocation2Context ctx, PLCTranslatorNew translatorNew){
        PLCVariable instanceSymbol = (PLCVariable) PLCTranslatorNew.properties.get(ctx.invocation2branch()).get(0);

//        System.out.println(instanceSymbol.getRuntimeName()+"::callFunc("+instanceSymbol.getName());

        for (PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()) {
            PLCVariable paraSymbol = (PLCVariable) PLCTranslatorNew.properties.get(param_assignContext).get(0);

//            System.out.print(","+paraSymbol.getRuntimeName()); //类内函数调用细化修改
        }
//        System.out.print(");");
        return null;
    }
}
