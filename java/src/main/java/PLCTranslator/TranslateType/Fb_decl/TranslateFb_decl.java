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
                sb.append("    ").append(f.typeName).append(" ").append(f.declName()).append(";");
                if (f.initValue != null && !f.initValue.isEmpty()) {
                    sb.append(" // = ").append(f.initValue);
                }
                sb.append("\n");
            }
            sb.append("\n    void update(TIME dt) {\n");
            String body = translatorNew.visit(ctx.fb_body());
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
}
