package PLCTranslator.TranslateType.Fb_decl;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateFb_decl {

    public String translateNode(PLCSTPARSERParser.Fb_declContext ctx, PLCTranslatorNew translatorNew) {
        PLCFBDeclSymbol fbSymbol = (PLCFBDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);
        String fbName = fbSymbol.getName();

        StringBuilder sb = new StringBuilder();

        List<FBField> fields = collectFields(ctx, translatorNew.gvlCtx);

        boolean hasBody = ctx.fb_body() != null
            && ctx.fb_body().stmt_list() != null
            && ctx.fb_body().stmt_list().stmt() != null
            && !ctx.fb_body().stmt_list().stmt().isEmpty();

        if (hasBody) {
            sb.append("\nstruct ").append(fbName).append(" {\n");
            for (FBField f : fields) {
                sb.append("    ").append(f.typeName).append(" ").append(f.declName());
                if (f.initValue != null && !f.initValue.isEmpty()) {
                    String init = f.initValue;
                    if (!init.contains(",") && !init.contains(":=") && !init.contains("{")) {
                        init = PLCVariable.stripParens(init);
                        sb.append(" = ").append(init);
                    } else if (init.contains(":=")) {
                        sb.append(" = ").append(toAggregateInit(init, f.typeName));
                    } else {
                        sb.append(" = ").append(init);
                    }
                }
                sb.append(";\n");
            }
            sb.append("\n    void update(GVL& gvl, ProcessImage& io, TIME dt) {\n");
            translatorNew.inFB = true;
            String body = translatorNew.visit(ctx.fb_body());
            translatorNew.inFB = false;
            if (body != null) sb.append(body);
            sb.append("\n    }\n");
            sb.append("};\n");
        } else {
            sb.append("\nstruct ").append(fbName).append(";\n");
        }

        List<GvlContext.StructField> structFields = new ArrayList<>();
        int currentOffset = 0;
        for (FBField f : fields) {
            String nativeType = translatorNew.gvlCtx.toNativeType(f.typeName);
            int elemSize = translatorNew.gvlCtx.getTypeSize(nativeType);
            int count = arrayCount(f.arraySuffix);
            int totalSize = elemSize * count;
            int aligned = (currentOffset + elemSize - 1) / elemSize * elemSize;
            structFields.add(new GvlContext.StructField(f.name, nativeType + f.arraySuffix, aligned));
            currentOffset = aligned + totalSize;
        }
        GvlContext.StructLayout layout = new GvlContext.StructLayout(
            fbName, structFields, currentOffset);
        translatorNew.gvlCtx.registerStructType(fbName, layout);

        return sb.toString();
    }

    private static class FBField {
        String name;
        String typeName;
        String initValue;
        String arraySuffix;
        FBField(String n, String t, String i) {
            name = n; typeName = t; initValue = i; arraySuffix = "";
        }
        FBField(String n, String t, String i, String suffix) {
            name = n; typeName = t; initValue = i;
            arraySuffix = (suffix != null) ? suffix : "";
        }
        String declName() { return name + arraySuffix; }
    }

    private List<FBField> collectFields(PLCSTPARSERParser.Fb_declContext ctx, GvlContext gvlCtx) {
        List<FBField> fields = new ArrayList<>();
        PLCFBDeclSymbol fbSymbol = (PLCFBDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);
        PLCSymbolTable importTable = fbSymbol.getImportSymbolTable();
        if (importTable == null) return fields;

        for (PLCSymbol s : importTable.getSymbolIDHashMap().values()) {
            if (s instanceof PLCVariable v) {
                if (v.getSymbolId() == fbSymbol.getSymbolId()) continue;
                String typeName = resolveFieldTypeName(v, gvlCtx);
                String suffix = makeArraySuffix(v);
                fields.add(new FBField(v.getName(), typeName, v.getAssignVar(), suffix));
            }
        }
        return fields;
    }

    private static String makeArraySuffix(PLCVariable v) {
        int[][] bounds = v.getArrayBounds();
        if (bounds == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int[] dim : bounds) {
            sb.append("[").append(dim[2]).append("]");
        }
        return sb.toString();
    }

    private static int arrayCount(String suffix) {
        if (suffix == null || suffix.isEmpty()) return 1;
        int total = 1;
        Matcher m = Pattern.compile("\\[(\\d+)\\]").matcher(suffix);
        while (m.find()) total *= Integer.parseInt(m.group(1));
        return total;
    }

    private static String resolveFieldTypeName(PLCVariable v, GvlContext gvlCtx) {
        int typeId = v.getTypeId();
        if (typeId == 0) return gvlCtx.toNativeType(v.getRuntimeTypeName());
        PLCTypeDeclSymbol typeDecl = PLCTotalSymbolTable.getTypeByTypeID(typeId);
        if (typeDecl == null) return gvlCtx.toNativeType(v.getRuntimeTypeName());
        if (typeDecl instanceof PLCEnumDeclSymbol) return typeDecl.getName();
        if (typeDecl instanceof PLCStructDeclSymbol) return typeDecl.getName();
        if (typeDecl instanceof PLCArrayDeclSymbol) {
            PLCArrayDeclSymbol arrayDecl = (PLCArrayDeclSymbol) typeDecl;
            PLCTypeDeclSymbol elemType = PLCTotalSymbolTable.getTypeByTypeID(
                arrayDecl.getElementTypeId());
            if (elemType != null) return gvlCtx.toNativeType(elemType.getName());
            return "INT";
        }
        return gvlCtx.toNativeType(typeDecl.getName());
    }

    /**
     * 将 ST 命名聚合初始化转为 C++ 聚合初始化。
     * (J1:=0.0,J2:=-45.0,SPEED:=50.0,DWELL_MS:=500) → {0.0, -45.0, ..., 50.0, 500}
     * 使用子 struct 的字段顺序，而非父 struct 的字段列表。
     */
    private static String toAggregateInit(String init, String childTypeName) {
        // 解析命名参数 → Map<name, value>
        java.util.LinkedHashMap<String, String> namedValues = new java.util.LinkedHashMap<>();
        String inner = PLCVariable.stripParens(init);
        for (String part : inner.split(",")) {
            String trimmed = part.trim();
            int eq = trimmed.indexOf(":=");
            if (eq >= 0) {
                namedValues.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 2).trim());
            }
        }
        // 查子 struct 的字段列表（按声明顺序）
        List<String> childFieldNames = new ArrayList<>();
        for (PLCTypeDeclSymbol t : PLCTotalSymbolTable.totalTypeMap.values()) {
            if (t instanceof PLCStructDeclSymbol s && t.getName().equals(childTypeName)) {
                for (PLCVariable v : s.getVariables()) {
                    childFieldNames.add(v.getName());
                }
                break;
            }
        }
        // 按子 struct 字段顺序输出值
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < childFieldNames.size(); i++) {
            if (i > 0) sb.append(", ");
            String val = namedValues.get(childFieldNames.get(i));
            sb.append(val != null ? val : "0");
        }
        sb.append("}");
        return sb.toString();
    }
}
