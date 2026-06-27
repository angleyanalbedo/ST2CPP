package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Array_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSubtypeDeclSymbol;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateArray_type_decl {

    public String translateNode(PLCSTPARSERParser.Array_type_declContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        PLCSubtypeDeclSymbol arraySymbol = (PLCSubtypeDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);

        // 生成 std::array 类型别名
        int lowerBound = 0;
        int upperBound = 0;
        String elemType = "INT";

        if (ctx.array_spec_init() != null && ctx.array_spec_init().array_spec() != null) {
            var arraySpec = ctx.array_spec_init().array_spec();
            if (arraySpec.data_type_access() != null) {
                elemType = arraySpec.data_type_access().getText();
                elemType = translatorNew.gvlCtx.toNativeType(elemType);
            }
            if (!arraySpec.subrange().isEmpty()) {
                var subrange = arraySpec.subrange(0);
                if (subrange.constant_expr(0) != null && subrange.constant_expr(1) != null) {
                    try {
                        lowerBound = Integer.parseInt(subrange.constant_expr(0).getText());
                        upperBound = Integer.parseInt(subrange.constant_expr(1).getText());
                    } catch (NumberFormatException e) { }
                }
            }
        }

        int size = upperBound - lowerBound + 1;
        sb.append("\nusing ").append(arraySymbol.getName()).append(" = std::array<")
          .append(elemType).append(", ").append(size).append(">;");

        return sb.toString();
    }
}
