package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Simple_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateSimple_type_decl {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Simple_type_declContext ctx, PLCTranslatorNew translatorNew){
        //typedef名称
        String typeName = ctx.simple_type_name().getText();

        //获取typedef类型名
        PLCVariable typedefSymbol =(PLCVariable) PLCTranslatorNew.properties.get(ctx.simple_spec_init()).get(0);

//        System.out.println("typedef "+ typedefSymbol.getRuntimeTypeName()+"<int, typeID>");
        return null;
    }
}
