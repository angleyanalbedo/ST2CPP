package PLCTranslator.TranslateType.Fb_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

public class TranslateFb_decl {

    public String translateNode(PLCSTPARSERParser.Fb_declContext ctx, PLCTranslatorNew translatorNew) {
        PLCFBDeclSymbol fbSymbol = (PLCFBDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);
        String fbName = fbSymbol.getName();
        FlatCodeGenerator flatGen = (FlatCodeGenerator) translatorNew.codeGen;

        StringBuilder sb = new StringBuilder();

        // 收集所有成员变量
        List<FBField> fields = collectFields(ctx);

        // 生成 C++ struct 定义
        sb.append("\nstruct ").append(fbName).append(" {\n");
        for (FBField f : fields) {
            String nativeType = flatGen.toNativeType(f.typeName);
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
        List<FlatCodeGenerator.StructField> structFields = new ArrayList<>();
        int currentOffset = 0;
        for (FBField f : fields) {
            String nativeType = flatGen.toNativeType(f.typeName);
            int fieldSize = flatGen.getTypeSize(nativeType);
            int aligned = (currentOffset + fieldSize - 1) / fieldSize * fieldSize;
            structFields.add(new FlatCodeGenerator.StructField(f.name, nativeType, aligned));
            currentOffset = aligned + fieldSize;
        }
        FlatCodeGenerator.StructLayout layout = new FlatCodeGenerator.StructLayout(
            fbName, structFields, currentOffset);
        flatGen.registerStructType(fbName, fbName, layout);

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

        // VAR_INPUT / VAR_OUTPUT / VAR_IN_OUT
        for (PLCSTPARSERParser.Fb_io_var_declsContext io : ctx.fb_io_var_decls()) {
            ArrayList<PLCSymbol> symbols = PLCTranslatorNew.properties.get(io);
            if (symbols != null) {
                for (PLCSymbol s : symbols) {
                    if (s instanceof PLCVariable v) {
                        fields.add(new FBField(v.getName(), v.getRuntimeTypeName(), v.getAssignVar()));
                    }
                }
            }
        }

        // VAR
        for (PLCSTPARSERParser.Func_var_declsContext fv : ctx.func_var_decls()) {
            ArrayList<PLCSymbol> symbols = PLCTranslatorNew.properties.get(fv);
            if (symbols != null) {
                for (PLCSymbol s : symbols) {
                    if (s instanceof PLCVariable v) {
                        fields.add(new FBField(v.getName(), v.getRuntimeTypeName(), v.getAssignVar()));
                    }
                }
            }
        }

        // VAR_TEMP
        for (PLCSTPARSERParser.Temp_var_declsContext tv : ctx.temp_var_decls()) {
            ArrayList<PLCSymbol> symbols = PLCTranslatorNew.properties.get(tv);
            if (symbols != null) {
                for (PLCSymbol s : symbols) {
                    if (s instanceof PLCVariable v) {
                        fields.add(new FBField(v.getName(), v.getRuntimeTypeName(), v.getAssignVar()));
                    }
                }
            }
        }

        // VAR_EXTERNAL / other
        for (PLCSTPARSERParser.Other_var_declsContext ov : ctx.other_var_decls()) {
            ArrayList<PLCSymbol> symbols = PLCTranslatorNew.properties.get(ov);
            if (symbols != null) {
                for (PLCSymbol s : symbols) {
                    if (s instanceof PLCVariable v) {
                        fields.add(new FBField(v.getName(), v.getRuntimeTypeName(), v.getAssignVar()));
                    }
                }
            }
        }

        return fields;
    }
}