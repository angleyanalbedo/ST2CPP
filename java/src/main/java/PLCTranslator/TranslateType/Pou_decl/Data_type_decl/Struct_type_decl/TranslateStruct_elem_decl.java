package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

/**
 * 翻译struct
 */
public class TranslateStruct_elem_decl {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Struct_elem_declContext ctx, PLCTranslatorNew translatorNew){
        String varName = ctx.struct_elem_name().getText();

        PLCVariable typeSymbol = (PLCVariable) PLCTranslatorNew.properties.get(ctx.getChild(1)).get(0);

//        System.out.println("auto* "+varName+"= new "+ typeSymbol.getRuntimeTypeName()+"("+
//                typeSymbol.getAssignVar()+");");

//        System.out.println("<structName>.addField(0,"+"\""+varName+"\""+","+varName+");");
        return null;
    }
}
