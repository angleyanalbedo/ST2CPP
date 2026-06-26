package PLCTranslator.TranslateType.Fb_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCTranslator.CodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

public class TranslateFb_decl {

    public String translateNode(PLCSTPARSERParser.Fb_declContext ctx, PLCTranslatorNew translatorNew) {
        PLCFBDeclSymbol fbSymbol = (PLCFBDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);
        String fbName = fbSymbol.getName();

        StringBuilder sb = new StringBuilder();

        // 收集所有成员变量
        List<FBField> fields = collectFields(ctx);

        // 生成 C++ struct 定义
        sb.append("\nstruct ").append(fbName).append(" {\n");
        for (FBField f : fields) {
            String nativeType = translatorNew.codeGen.toNativeType(f.typeName);
            sb.append("    ").append(nativeType).append(" ").append(f.name).append(";");
            if (f.initValue != null && !f.initValue.isEmpty()) {
                sb.append(" // = ").append(f.initValue);
            }
            sb.append("\n");
        }

        // 生成 update() 方法
        sb.append("\n    void update() {\n");
        if (ctx.fb_body() != null) {
            String body = translatorNew.visit(ctx.fb_body());
            if (body != null) sb.append(body);
        }
        sb.append("\n    }\n");

        sb.append("};\n");

        // 注册 struct 布局（用于字段偏移计算）
        List<CodeGenerator.StructField> structFields = new ArrayList<>();
        int currentOffset = 0;
        for (FBField f : fields) {
            String nativeType = translatorNew.codeGen.toNativeType(f.typeName);
            int fieldSize = translatorNew.codeGen.getTypeSize(nativeType);
            int aligned = (currentOffset + fieldSize - 1) / fieldSize * fieldSize;
            structFields.add(new CodeGenerator.StructField(f.name, nativeType, aligned));
            currentOffset = aligned + fieldSize;
        }
        CodeGenerator.StructLayout layout = new CodeGenerator.StructLayout(
            fbName, structFields, currentOffset);
        translatorNew.codeGen.registerStructType(fbName, fbName, layout);

        return sb.toString();
    }

    private static class FBField {
        String name;
        String typeName;
        String initValue;
        FBField(String n, String t, String i) { name = n; typeName = t; initValue = i; }
    }

    private List<FBField> collectFields(PLCSTPARSERParser.Fb_declContext ctx) {
        List<FBField> fields = new ArrayList<>();
        PLCFBDeclSymbol fbSymbol = (PLCFBDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);
        PLCSymbolTable importTable = fbSymbol.getImportSymbolTable();
        if (importTable == null) return fields;

        for (PLCSymbol s : importTable.getSymbolIDHashMap().values()) {
            if (s instanceof PLCVariable v) {
                if (v.getSymbolId() == fbSymbol.getSymbolId()) continue;
                fields.add(new FBField(v.getName(), v.getRuntimeTypeName(), v.getAssignVar()));
            }
        }
        return fields;
    }
}
