package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Array_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCArrayDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSubtypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

public class TranslateArray_type_decl {

    packageFactory pFactory = new packageFactory();

    public ArrayList<String> translateNode(PLCSTPARSERParser.Array_type_declContext ctx, PLCTranslatorNew translatorNew){

        PLCSubtypeDeclSymbol arraySymbol = (PLCSubtypeDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);

        //数组类型创建
//        System.out.println(pFactory.packageArrayTypeInitSentences(arraySymbol.getName()));
        writeTarget("\n"+pFactory.packageArrayTypeInitSentences(arraySymbol.getName()));
        ArrayList<PLCSymbol> arrayInitList = PLCTranslatorNew.properties.get(ctx.array_spec_init());

        for (PLCSymbol symbol : arrayInitList) {
            PLCVariable tempSymbol = (PLCVariable) symbol;
//            System.out.println(pFactory.packageArrayElementAddSentences("*(new "+tempSymbol.getAssignVar()+")",
//                    arraySymbol.getName()));
            writeTarget("\n"+pFactory.packageArrayElementAddSentences("*(new "+tempSymbol.getAssignVar()+")",
                    arraySymbol.getName()));

        }

//        System.out.println("PLC_Array_Type<"+String.valueOf(arraySymbol.getTypeId())+"> "+ arraySymbol.getName()+"_"
//                +"= new "+"PLC_Array_Type<"+String.valueOf(arraySymbol.getTypeId())+">(0,v"+arraySymbol.getName()+");");
        writeTarget("\nPLC_Array_Type<"+String.valueOf(arraySymbol.getTypeId())+"> "+ arraySymbol.getName()+"_"
                +"= new "+"PLC_Array_Type<"+String.valueOf(arraySymbol.getTypeId())+">(0,v"+arraySymbol.getName()+");");
//        System.out.println(pFactory.packageTypedefSentences("PLC_Array<"
//                +String.valueOf(arraySymbol.getTypeId())+">", arraySymbol.getName()));
        writeTarget("\n"+pFactory.packageTypedefSentences("PLC_Array<"
                +String.valueOf(arraySymbol.getTypeId())+">", arraySymbol.getName()));
        return null;
    }
}
