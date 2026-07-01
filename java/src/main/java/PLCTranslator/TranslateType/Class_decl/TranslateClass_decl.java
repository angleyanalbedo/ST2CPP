package PLCTranslator.TranslateType.Class_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateClass_decl {

    public String translateNode(PLCSTPARSERParser.Class_declContext ctx, PLCTranslatorNew translatorNew){
        PLCClassDeclSymbol classDeclSymbol = (PLCClassDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);
        String className = classDeclSymbol.getName();

        translatorNew.inClassDecl = true;

        StringBuilder sb = new StringBuilder();
        sb.append("\nclass ").append(className).append(" {");
        sb.append("\npublic:");

        List<GvlContext.StructField> structFields = new ArrayList<>();
        int currentOffset = 0;
        for (PLCVariable value : classDeclSymbol.getVariableMap().values()) {
            String nativeType = translatorNew.gvlCtx.toNativeType(value.getRuntimeTypeName());
            String arraySuffix = makeArraySuffix(value);
            int elemSize = translatorNew.gvlCtx.getTypeSize(nativeType);
            int count = arrayCount(arraySuffix);
            int totalSize = elemSize * count;
            int aligned = (currentOffset + elemSize - 1) / elemSize * elemSize;

            sb.append("\n\t").append(nativeType).append(" ").append(value.getName()).append(arraySuffix).append(";");
            structFields.add(new GvlContext.StructField(value.getName(), nativeType + arraySuffix, aligned));
            currentOffset = aligned + totalSize;
        }

        for (PLCSTPARSERParser.Method_declContext method_declContext : ctx.method_decl()) {
            String result = translatorNew.visit(method_declContext);
            sb.append(result);
        }

        sb.append("\n};");
        translatorNew.inClassDecl = false;

        GvlContext.StructLayout layout = new GvlContext.StructLayout(className, structFields, currentOffset);
        translatorNew.gvlCtx.registerStructType(className, layout);

        return sb.toString();
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

}
