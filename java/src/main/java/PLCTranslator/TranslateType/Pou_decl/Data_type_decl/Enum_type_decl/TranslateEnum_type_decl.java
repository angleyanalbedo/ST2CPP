package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Enum_type_decl;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCEnumDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

//枚举类型定义翻译

public class TranslateEnum_type_decl {
    public packageFactory pFactory = new packageFactory();

    public ArrayList<String> translateNode(PLCSTPARSERParser.Enum_type_declContext ctx, PLCTranslatorNew translatorNew){
        String enumName = ctx.enum_type_name().getText()+"_";

        PLCEnumDeclSymbol enumSymbol =(PLCEnumDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);

        //枚举类型声明
//        System.out.println(pFactory.packageEnumTypeDeclSentences
//                (String.valueOf(enumSymbol.getTypeId()), enumName));
        writeTarget("\n"+pFactory.packageEnumTypeDeclSentences
                (String.valueOf(enumSymbol.getTypeId()), enumName));

        ArrayList<PLCVariable> enumInitList = enumSymbol.getEnumValues();


        //f访问总表
        int typeId = enumInitList.get(0).getTypeId();

        PLCTypeDeclSymbol typeByTypeID = PLCTotalSymbolTable.getTypeByTypeID(typeId);

        String typeStr = typeByTypeID.getRuntimeName();
        for (PLCVariable variable : enumInitList) {
//            System.out.println(pFactory.packageEnumElementAddSentences(enumName,typeStr
//                , variable.getName(), variable.getAssignVar()));
            writeTarget("\n"+pFactory.packageEnumElementAddSentences(enumName,typeStr
                    , variable.getName(), variable.getAssignVar()));
        }

//        System.out.println(pFactory.packageTypedefSentences("PLC_Enum_Value<"+String.valueOf(enumSymbol.getTypeId())+">"
//                , enumSymbol.getName()));
        writeTarget("\n"+pFactory.packageTypedefSentences("PLC_Enum_Value<"+String.valueOf(enumSymbol.getTypeId())+">"
                , enumSymbol.getName()));
        return null;
    }
}
