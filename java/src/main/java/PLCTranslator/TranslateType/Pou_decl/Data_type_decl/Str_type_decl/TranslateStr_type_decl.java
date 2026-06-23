package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Str_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateStr_type_decl {

    public ArrayList<String> translateNode(PLCSTPARSERParser.Str_type_declContext ctx, PLCTranslatorNew translatorNew){
        //typedef名称
        String typeName = ctx.string_type_name_identifier().getText();

        //获取typedef类型名
        PLCVariable typedefSymbol =(PLCVariable) PLCTranslatorNew.properties.get(ctx.str_spec()).get(0);

//        System.out.println("typedef "+ typedefSymbol.getRuntimeTypeName()+"<String, typeID>");
        return null;
    }
}
