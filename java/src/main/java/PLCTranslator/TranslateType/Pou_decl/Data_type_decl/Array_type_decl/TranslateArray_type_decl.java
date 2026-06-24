package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Array_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCArrayDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSubtypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateArray_type_decl {

    packageFactory pFactory = new packageFactory();

    public String translateNode(PLCSTPARSERParser.Array_type_declContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        PLCSubtypeDeclSymbol arraySymbol = (PLCSubtypeDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);
        boolean isFlat = translatorNew.codeGen instanceof FlatCodeGenerator;

        if (isFlat) {
            // Flat 模式：生成 std::array 类型别名
            // 从语法树解析数组维度
            int lowerBound = 0;
            int upperBound = 0;
            String elemType = "INT";

            if (ctx.array_spec_init() != null && ctx.array_spec_init().array_spec() != null) {
                var arraySpec = ctx.array_spec_init().array_spec();
                // 获取元素类型
                if (arraySpec.data_type_access() != null) {
                    elemType = arraySpec.data_type_access().getText();
                    elemType = ((FlatCodeGenerator) translatorNew.codeGen).toNativeType(elemType);
                }
                // 获取下界和上界（只支持一维）
                if (!arraySpec.subrange().isEmpty()) {
                    var subrange = arraySpec.subrange(0);
                    if (subrange.constant_expr(0) != null && subrange.constant_expr(1) != null) {
                        try {
                            lowerBound = Integer.parseInt(subrange.constant_expr(0).getText());
                            upperBound = Integer.parseInt(subrange.constant_expr(1).getText());
                        } catch (NumberFormatException e) {
                            // 使用默认值
                        }
                    }
                }
            }

            int size = upperBound - lowerBound + 1;
            sb.append("\nusing ").append(arraySymbol.getName()).append(" = std::array<")
              .append(elemType).append(", ").append(size).append(">;");

            // 注册数组类型信息到 FlatCodeGenerator（用于变量声明时计算大小）
            // TODO: 需要扩展 FlatCodeGenerator 支持数组类型注册

        } else {
            // OOP 模式（原有逻辑）
            sb.append("\n"+pFactory.packageArrayTypeInitSentences(arraySymbol.getName()));
            ArrayList<PLCSymbol> arrayInitList = PLCTranslatorNew.properties.get(ctx.array_spec_init());

            if (arrayInitList != null) {
                for (PLCSymbol symbol : arrayInitList) {
                    PLCVariable tempSymbol = (PLCVariable) symbol;
                    sb.append("\n"+pFactory.packageArrayElementAddSentences("*(new "+translatorNew.codeGen.translateExpr(tempSymbol.getAssignVar())+")",
                            arraySymbol.getName()));

                }
            }

            sb.append("\nPLC_Array_Type<"+String.valueOf(arraySymbol.getTypeId())+"> "+ arraySymbol.getName()+"_"
                    +"= new "+"PLC_Array_Type<"+String.valueOf(arraySymbol.getTypeId())+">(0,v"+arraySymbol.getName()+");");
            sb.append("\n"+pFactory.packageTypedefSentences("PLC_Array<"
                    +String.valueOf(arraySymbol.getTypeId())+">", arraySymbol.getName()));
        }
        return sb.toString();
    }
}
