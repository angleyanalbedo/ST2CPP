package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Derived_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
//搁置
public class TranslateDerived_type_decl {

    public ArrayList<String> translateNode(PLCSTPARSERParser.Derived_type_declContext ctx, PLCTranslatorNew translatorNew){
        //typedef名称
        String typeName = ctx.derived_type_name().getText();

        //获取typedef类型名
        PLCVariable typedefSymbol =(PLCVariable) PLCTranslatorNew.properties.get(ctx.derived_spec_init()).get(0);

//        System.out.println("typedef "+ typedefSymbol.getRuntimeName()+"<"+typedefSymbol.getRuntimeTypeName()
//                +","+typedefSymbol.getTypeId()+">"+typeName+";");
        return null;
    }
}
