package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

public class TranslateStruct_type_decl {

    packageFactory pFactory = new packageFactory();

    public ArrayList<String> translateNode(PLCSTPARSERParser.Struct_type_declContext ctx, PLCTranslatorNew translatorNew){
        String structName = ctx.struct_type_name().getText()+"_";

        PLCStructDeclSymbol structSymbol = (PLCStructDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);

//        System.out.println(pFactory.packageStructTypeDecl(String.valueOf(structSymbol.getTypeId()), structName));
        writeTarget("\n"+pFactory.packageStructTypeDecl(String.valueOf(structSymbol.getTypeId()), structName));
        for (PLCStructDeclSymbol structDeclSymbol : structSymbol.getStructElements()) {
//            System.out.println(pFactory.packageStructElementAddSentences(structName, structDeclSymbol.getName(),structDeclSymbol.getInitVar()));
            writeTarget("\n"+pFactory.packageStructElementAddSentences(structName, structDeclSymbol.getName(),structDeclSymbol.getInitVar()));
        }
//        System.out.println(pFactory.packageTypedefSentences("PLC_Struct_Value<"+structSymbol.getTypeId()
//                +">",structSymbol.getName().toUpperCase()));
        writeTarget("\n"+pFactory.packageTypedefSentences("PLC_Struct_Value<"+structSymbol.getTypeId()
                +">",structSymbol.getName().toUpperCase()));
        return null;
    }
}
