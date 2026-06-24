package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Array_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCArrayDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSubtypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateArray_type_decl {

    packageFactory pFactory = new packageFactory();

    public String translateNode(PLCSTPARSERParser.Array_type_declContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        PLCSubtypeDeclSymbol arraySymbol = (PLCSubtypeDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);

        //数组类型创建
        sb.append("\n"+pFactory.packageArrayTypeInitSentences(arraySymbol.getName()));
        ArrayList<PLCSymbol> arrayInitList = PLCTranslatorNew.properties.get(ctx.array_spec_init());

        for (PLCSymbol symbol : arrayInitList) {
            PLCVariable tempSymbol = (PLCVariable) symbol;
            sb.append("\n"+pFactory.packageArrayElementAddSentences("*(new "+translatorNew.codeGen.translateExpr(tempSymbol.getAssignVar())+")",
                    arraySymbol.getName()));

        }

        sb.append("\nPLC_Array_Type<"+String.valueOf(arraySymbol.getTypeId())+"> "+ arraySymbol.getName()+"_"
                +"= new "+"PLC_Array_Type<"+String.valueOf(arraySymbol.getTypeId())+">(0,v"+arraySymbol.getName()+");");
        sb.append("\n"+pFactory.packageTypedefSentences("PLC_Array<"
                +String.valueOf(arraySymbol.getTypeId())+">", arraySymbol.getName()));
        return sb.toString();
    }
}
