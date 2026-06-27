package PLCTranslator.TranslateType;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCEnumDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

public class TranslateEnum_type_decl {

    public String translate(PLCSTPARSERParser.Enum_type_declContext ctx,
                            GvlContext gvlCtx) {
        PLCEnumDeclSymbol enumSymbol = (PLCEnumDeclSymbol)
            PLCTranslatorNew.properties.get(ctx).get(0);
        String enumName = enumSymbol.getName();

        int protoTypeId = enumSymbol.getEnumConstTypeId();
        PLCTypeDeclSymbol protoType = PLCTotalSymbolTable.getTypeByTypeID(protoTypeId);
        String underlyingType = protoType != null
            ? gvlCtx.toNativeType(protoType.getRuntimeName()) : "INT";

        gvlCtx.registerEnumType(enumName, underlyingType);

        List<String> entries = new ArrayList<>();
        for (PLCVariable var : enumSymbol.getEnumValues()) {
            String valueExpr = stripParens(var.getAssignVar());
            if (valueExpr.equals("0")) {
                entries.add(var.getName());
            } else {
                entries.add(var.getName() + " = " + valueExpr);
            }
        }
        return gvlCtx.emitEnumDecl(enumName, underlyingType, entries);
    }

    private String stripParens(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("(") && s.endsWith(")")) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }
}
