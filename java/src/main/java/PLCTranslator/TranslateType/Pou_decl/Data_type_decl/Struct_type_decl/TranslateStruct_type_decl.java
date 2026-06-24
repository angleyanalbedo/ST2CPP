package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateStruct_type_decl {

    packageFactory pFactory = new packageFactory();

    public String translateNode(PLCSTPARSERParser.Struct_type_declContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        String structName = ctx.struct_type_name().getText()+"_";

        PLCStructDeclSymbol structSymbol = (PLCStructDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);

        sb.append("\n"+pFactory.packageStructTypeDecl(String.valueOf(structSymbol.getTypeId()), structName));
        for (PLCStructDeclSymbol structDeclSymbol : structSymbol.getStructElements()) {
            sb.append("\n"+pFactory.packageStructElementAddSentences(structName, structDeclSymbol.getName(),structDeclSymbol.getInitVar()));
        }
        sb.append("\n"+pFactory.packageTypedefSentences("PLC_Struct_Value<"+structSymbol.getTypeId()
                +">",structSymbol.getName().toUpperCase()));
        return sb.toString();
    }
}
