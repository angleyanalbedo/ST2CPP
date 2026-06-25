package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Derived_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

//搁置
public class TranslateDerived_type_decl {

    public String translateNode(PLCSTPARSERParser.Derived_type_declContext ctx, PLCTranslatorNew translatorNew){
        //typedef名称
        String typeName = ctx.derived_type_name().getText();

        //获取typedef类型名
        PLCVariable typedefSymbol = PLCTranslatorNew.getVariable(ctx.derived_spec_init(), "derived type spec");

        return "";
    }
}
