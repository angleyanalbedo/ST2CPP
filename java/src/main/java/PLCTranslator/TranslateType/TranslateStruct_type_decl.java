package PLCTranslator.TranslateType;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构体类型声明翻译。
 * 通过 typeId 查询 symbol 表获取字段类型信息，不依赖字符串解析。
 */
public class TranslateStruct_type_decl {

    /**
     * 根据字段 PLCVariable 获取 C++ 原生类型名。
     * 通过 typeId 递归查找类型声明符号，正确处理枚举、结构体、数组等。
     */
    public static String resolveFieldType(PLCVariable fieldVar, GvlContext gvlCtx) {
        int typeId = fieldVar.getTypeId();
        if (typeId == 0) {
            // 没有类型信息，回退到 runtimeTypeName
            return gvlCtx.toNativeType(fieldVar.getRuntimeTypeName());
        }

        PLCTypeDeclSymbol typeDecl = PLCTotalSymbolTable.getTypeByTypeID(typeId);
        if (typeDecl == null) {
            return gvlCtx.toNativeType(fieldVar.getRuntimeTypeName());
        }

        // 枚举类型 → 直接用枚举名（C++ 中就是 enum 名）
        if (typeDecl instanceof PLCEnumDeclSymbol) {
            return typeDecl.getName();
        }

        // 结构体类型 → 直接用结构体名
        if (typeDecl instanceof PLCStructDeclSymbol) {
            return typeDecl.getName();
        }

        // 数组类型 → 返回元素类型（数组维度由 arrayBounds 处理）
        if (typeDecl instanceof PLCArrayDeclSymbol) {
            PLCArrayDeclSymbol arrayDecl = (PLCArrayDeclSymbol) typeDecl;
            PLCTypeDeclSymbol elemType = PLCTotalSymbolTable.getTypeByTypeID(
                arrayDecl.getElementTypeId());
            if (elemType != null) {
                return gvlCtx.toNativeType(elemType.getName());
            }
            return "INT"; // fallback
        }

        // 基础类型（INT, DINT, REAL, BOOL 等）
        return gvlCtx.toNativeType(typeDecl.getName());
    }

    public String translate(PLCSTPARSERParser.Struct_type_declContext ctx,
                            GvlContext gvlCtx) {
        PLCStructDeclSymbol structSymbol = (PLCStructDeclSymbol)
            PLCTranslatorNew.properties.get(ctx).get(0);
        String structName = structSymbol.getName();

        StringBuilder sb = new StringBuilder();
        sb.append("\nstruct ").append(structName).append(" {");

        List<GvlContext.StructField> fields = new ArrayList<>();
        int currentOffset = 0;

        for (PLCVariable fieldVar : structSymbol.getVariables()) {
            String fieldName = fieldVar.getName();
            String fieldType = resolveFieldType(fieldVar, gvlCtx);
            int[][] arrayBounds = fieldVar.getArrayBounds();
            int fieldSize;

            if (arrayBounds != null) {
                // 数组字段：通过 arrayBounds 计算总元素数
                int totalCount = 1;
                for (int[] dim : arrayBounds) {
                    totalCount *= dim[2];
                }
                fieldSize = totalCount * gvlCtx.getTypeSize(fieldType);
                int aligned = fieldSize <= 1 ? currentOffset
                    : (currentOffset + fieldSize - 1) / fieldSize * fieldSize;
                fields.add(new GvlContext.StructField(fieldName, fieldType, aligned));
                sb.append("\n    ").append(fieldType).append(" ")
                  .append(fieldName).append("[").append(totalCount).append("];");
                currentOffset = aligned + fieldSize;
            } else {
                // 标量/结构体/枚举字段
                fieldSize = gvlCtx.getTypeSize(fieldType);
                int aligned = fieldSize <= 1 ? currentOffset
                    : (currentOffset + fieldSize - 1) / fieldSize * fieldSize;
                fields.add(new GvlContext.StructField(fieldName, fieldType, aligned));
                sb.append("\n    ").append(fieldType).append(" ")
                  .append(fieldName).append(";");
                currentOffset = aligned + fieldSize;
            }
        }

        sb.append("\n};\n");

        GvlContext.StructLayout layout =
            new GvlContext.StructLayout(structName, fields, currentOffset);
        gvlCtx.registerStructType(structName, structSymbol.getRuntimeTypeName(), layout);
        return sb.toString();
    }
}
