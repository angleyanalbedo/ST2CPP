package PLCTranslator.TranslateType.Prog_decl;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateProg_decl {

    public String translateNode(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();

        String progName = ctx.prog_type_name().identifier().getText();
        translatorNew.gvlCtx.addProgramName(progName);

        String mangled = translatorNew.gvlCtx.mangleProgName(progName);

        sb.append("\nvoid PROGRAM_").append(mangled).append("_init(GVL& gvl, ProcessImage& io) {");
        sb.append(emitVarInit(ctx, translatorNew));
        sb.append(emitRetainRegion(ctx, translatorNew));
        sb.append(emitInitRetainLoad(ctx, translatorNew));
        sb.append("\n}");

        sb.append("\nvoid PROGRAM_").append(mangled).append("_pre(GVL& gvl, ProcessImage& io) {");
        sb.append("\n}");

        sb.append("\nvoid PROGRAM_").append(mangled).append("_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {");
        if (translatorNew.localCache) {
            // 开头：声明局部变量 + 从 GVL 加载
            sb.append(emitCyclicPrologue(translatorNew));
            // 设置 inCyclic 标志，让变量翻译直接使用变量名
            translatorNew.inCyclic = true;
        }
        String result = translatorNew.visit(ctx.fb_body());
        translatorNew.inCyclic = false;
        sb.append(result);
        if (translatorNew.localCache) {
            // 结尾：将局部变量写回 GVL
            sb.append(emitCyclicEpilogue(translatorNew));
        }
        sb.append("\n}");

        sb.append("\nvoid PROGRAM_").append(mangled).append("_post(GVL& gvl, ProcessImage& io) {");
        sb.append(emitPostRetainSave(ctx, translatorNew));
        sb.append("\n}");

        return sb.toString();
    }

    /**
     * 生成变量初始化语句（GVL 写入 + I/O 映射注册）
     */
    private String emitVarInit(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();

        for (PLCSTPARSERParser.Func_var_declsContext func_var_decl : ctx.func_var_decls()) {
            ArrayList<PLCSymbol> funcVarDecl = PLCTranslatorNew.properties.get(func_var_decl);
            for (PLCSymbol symbol : funcVarDecl) {
                if (symbol instanceof PLCVariable varSymbol) {
                    emitOneVarInit(sb, varSymbol, translatorNew);
                }
            }
        }

        for (PLCSTPARSERParser.Temp_var_declsContext temp_var_decl : ctx.temp_var_decls()) {
            ArrayList<PLCSymbol> tempVarDecl = PLCTranslatorNew.properties.get(temp_var_decl);
            for (PLCSymbol symbol : tempVarDecl) {
                if (symbol instanceof PLCVariable varSymbol) {
                    emitOneVarInit(sb, varSymbol, translatorNew);
                }
            }
        }

        for (PLCSTPARSERParser.Other_var_declsContext other_var_decl : ctx.other_var_decls()) {
            ArrayList<PLCSymbol> otherVarDecl = PLCTranslatorNew.properties.get(other_var_decl);
            for (PLCSymbol symbol : otherVarDecl) {
                if (symbol instanceof PLCVariable varSymbol) {
                    emitOneVarInit(sb, varSymbol, translatorNew);
                }
            }
        }

        // 处理 loc_var_decls（VAR ... AT 地址 声明）
        for (PLCSTPARSERParser.Loc_var_declsContext loc_var_decls : ctx.loc_var_decls()) {
            for (PLCSTPARSERParser.Loc_var_declContext locVarDecl : loc_var_decls.loc_var_decl()) {
                ArrayList<PLCSymbol> locVarList = PLCTranslatorNew.properties.get(locVarDecl);
                for (PLCSymbol symbol : locVarList) {
                    if (symbol instanceof PLCVariable varSymbol) {
                        emitOneVarInit(sb, varSymbol, translatorNew);
                    }
                }
            }
        }

        // 处理 io_var_decls（VAR_INPUT / VAR_OUTPUT / VAR_IN_OUT）
        for (PLCSTPARSERParser.Io_var_declsContext io_var_decls : ctx.io_var_decls()) {
            // input_decls, output_decls, in_out_decls 都使用 var_decl_init
            // 通过 io_var_decls 下的 input_decls / output_decls / in_out_decls 获取变量
            if (io_var_decls.input_decls() != null) {
                for (PLCSTPARSERParser.Input_declContext inputDecl : io_var_decls.input_decls().input_decl()) {
                    ArrayList<PLCSymbol> symbols = PLCTranslatorNew.properties.get(inputDecl);
                    for (PLCSymbol symbol : symbols) {
                        if (symbol instanceof PLCVariable varSymbol) {
                            emitOneVarInit(sb, varSymbol, translatorNew);
                        }
                    }
                }
            }
            if (io_var_decls.output_decls() != null) {
                for (PLCSTPARSERParser.Output_declContext outputDecl : io_var_decls.output_decls().output_decl()) {
                    ArrayList<PLCSymbol> symbols = PLCTranslatorNew.properties.get(outputDecl);
                    for (PLCSymbol symbol : symbols) {
                        if (symbol instanceof PLCVariable varSymbol) {
                            emitOneVarInit(sb, varSymbol, translatorNew);
                        }
                    }
                }
            }
            if (io_var_decls.in_out_decls() != null) {
                for (PLCSTPARSERParser.In_out_var_declContext inOutDecl : io_var_decls.in_out_decls().in_out_var_decl()) {
                    ArrayList<PLCSymbol> symbols = PLCTranslatorNew.properties.get(inOutDecl);
                    for (PLCSymbol symbol : symbols) {
                        if (symbol instanceof PLCVariable varSymbol) {
                            emitOneVarInit(sb, varSymbol, translatorNew);
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * 处理单个变量：有 AT 地址的注册为 I/O 映射变量，否则分配 GVL 偏移量
     */
    private void emitOneVarInit(StringBuilder sb, PLCVariable varSymbol, PLCTranslatorNew translatorNew) {
        String location = varSymbol.getLocation();
        if (location != null && !location.isEmpty()) {
            translatorNew.gvlCtx.registerIOVariable(
                varSymbol.getName(),
                varSymbol.getRuntimeTypeName(),
                location
            );
        } else {
            emitVarDeclInline(sb, varSymbol, translatorNew.gvlCtx);
        }
    }

    /**
     * 生成 RETAIN 区域标记
     */
    private String emitRetainRegion(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();
        java.util.ArrayList<PLCVariable> retainVars = new java.util.ArrayList<>();

        for (PLCSTPARSERParser.Other_var_declsContext other_var_decl : ctx.other_var_decls()) {
            ArrayList<PLCSymbol> otherVarDecl = PLCTranslatorNew.properties.get(other_var_decl);
            for (PLCSymbol symbol : otherVarDecl) {
                if (symbol instanceof PLCVariable varSymbol) {
                    if (varSymbol.getRetainQualifiers() == PLCModifierEnum.RetainModifier.RETAIN) {
                        retainVars.add(varSymbol);
                    }
                }
            }
        }

        if (!retainVars.isEmpty()) {
            java.util.Map<String, Integer> offsets = translatorNew.gvlCtx.getOffsetMap();
            java.util.Map<String, String> types = translatorNew.gvlCtx.getTypeMap();

            int retainStart = Integer.MAX_VALUE;
            int retainEnd = 0;
            for (PLCVariable rv : retainVars) {
                if (translatorNew.gvlCtx.isIOVariable(rv.getName())) continue;
                Integer off = offsets.get(rv.getName());
                if (off != null) {
                    retainStart = Math.min(retainStart, off);
                    String type = types.getOrDefault(rv.getName(), "INT");
                    int size = translatorNew.gvlCtx.getTypeSize(type);
                    retainEnd = Math.max(retainEnd, off + size);
                }
            }
            if (retainStart < Integer.MAX_VALUE && retainEnd > retainStart) {
                sb.append("\n\t\tgvl.setRetainRegion(").append(retainStart).append(", ").append(retainEnd).append(");");
            }
        }

        return sb.toString();
    }

    /**
     * _init：如果有 RETAIN 变量，从备份缓冲区恢复
     */
    private String emitInitRetainLoad(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();
        for (PLCSTPARSERParser.Other_var_declsContext other_var_decl : ctx.other_var_decls()) {
            ArrayList<PLCSymbol> otherVarDecl = PLCTranslatorNew.properties.get(other_var_decl);
            for (PLCSymbol symbol : otherVarDecl) {
                if (symbol instanceof PLCVariable varSymbol) {
                    if (varSymbol.getRetainQualifiers() == PLCModifierEnum.RetainModifier.RETAIN) {
                        sb.append("\n\tgvl.loadRetain();");
                        return sb.toString();
                    }
                }
            }
        }
        return sb.toString();
    }

    private void emitVarDeclInline(StringBuilder sb, PLCVariable varSymbol, GvlContext gvlCtx) {
        String name = varSymbol.getName();
        String typeName = varSymbol.getRuntimeTypeName();
        int typeId = varSymbol.getTypeId();
        int[][] arrayBounds = varSymbol.getArrayBounds();

        if (arrayBounds != null) {
            // 数组类型：通过 arrayBounds 计算 count 和 elemType
            PLCArrayDeclSymbol arrayDecl = (PLCArrayDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(typeId);
            String elemTypeNative = "INT";
            if (arrayDecl != null) {
                PLCTypeDeclSymbol elemTypeDecl = PLCTotalSymbolTable.getTypeByTypeID(arrayDecl.getElementTypeId());
                if (elemTypeDecl != null) {
                    elemTypeNative = gvlCtx.toNativeType(elemTypeDecl.getName());
                }
            }
            int totalCount = varSymbol.getArrayTotalCount();
            int elemSize = gvlCtx.getTypeSize(elemTypeNative);
            int totalSize = elemSize * totalCount;
            gvlCtx.allocateArrayOffset(name, totalCount, elemTypeNative, arrayBounds);
            return;
        }

        // 枚举变量不分配 GVL 偏移
        if (typeId != 0) {
            PLCTypeDeclSymbol typeDecl = PLCTotalSymbolTable.getTypeByTypeID(typeId);
            if (typeDecl instanceof PLCEnumDeclSymbol) {
                return;
            }
        }

        String nativeType = gvlCtx.toNativeType(typeName);
        gvlCtx.allocateOffset(name, nativeType);
        String assignVar = varSymbol.getAssignVar();
        if (assignVar != null && !assignVar.isEmpty() && !"0".equals(assignVar) && !"\"\"".equals(assignVar)) {
            String initValue = assignVar.contains(":=") ? toAggregateInit(assignVar) : PLCVariable.stripParens(assignVar);
            String type = gvlCtx.typeMap.get(name);
            Integer offset = gvlCtx.offsetMap.get(name);
            if (type != null && offset != null) {
                sb.append("\n\t\tgvl.write<").append(type).append(">(")
                  .append(offset).append(", ").append(initValue).append(");");
            } else {
                sb.append("\n\t\t").append(name).append(" = ").append(initValue).append(";");
            }
        }
    }

    /**
     * 将 ST 命名参数初始化转为 C++ 聚合初始化。
     * 例: (KP:=2.0,KI:=0.5) → {2.0, 0.5}
     * 非命名参数保持原样。
     */
    private String toAggregateInit(String initExpr) {
        if (initExpr == null) return "";
        String inner = PLCVariable.stripParens(initExpr);
        if (inner.isEmpty()) return "{}";
        String[] parts = inner.split(",");
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            int colonEq = part.indexOf(":=");
            if (colonEq >= 0) {
                part = part.substring(colonEq + 2).trim();
            }
            if (i > 0) sb.append(", ");
            sb.append(part);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * cyclic 开头：为每个 GVL 变量生成局部 C++ 变量声明并从 GVL 加载
     */
    private String emitCyclicPrologue(PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();
        GvlContext gvlCtx = translatorNew.gvlCtx;
        for (java.util.Map.Entry<String, Integer> entry : gvlCtx.offsetMap.entrySet()) {
            String varName = entry.getKey();
            int offset = entry.getValue();
            String type = gvlCtx.typeMap.get(varName);
            if (type == null) continue;
            if (gvlCtx.ioVarMap.containsKey(varName)) continue;
            if (type.startsWith("ARRAY[")) {
                int[][] bounds = gvlCtx.arrayBoundsMap.get(varName);
                String elemType = gvlCtx.arrayElemTypeMap.getOrDefault(varName, "INT");
                int totalCount = (bounds != null) ? bounds[0][2] : 0;
                if (totalCount > 0) {
                    int elemSize = gvlCtx.getTypeSize(elemType);
                    sb.append("\n\t").append(elemType).append(" ")
                      .append(varName).append("[").append(totalCount).append("];");
                    for (int i = 0; i < totalCount; i++) {
                        int elemOffset = offset + i * elemSize;
                        sb.append("\n\t").append(varName).append("[").append(i)
                          .append("] = gvl.read<").append(elemType).append(">(")
                          .append(elemOffset).append(");");
                    }
                }
                continue;
            }
            sb.append("\n\t").append(type).append(" ").append(varName).append(" = gvl.read<")
              .append(type).append(">(").append(offset).append(");");
            gvlCtx.locallyDeclaredGvlVars.add(varName);
        }
        for (java.util.Map.Entry<String, GvlContext.IOInfo> ioEntry : gvlCtx.ioVarMap.entrySet()) {
            String varName = ioEntry.getKey();
            GvlContext.IOInfo info = ioEntry.getValue();
            if (info.bitOffset >= 0) {
                sb.append("\n\tBOOL ").append(varName).append(" = io.");
                sb.append(info.dir == GvlContext.IODirection.INPUT ? "readInputBit(" : "readOutputBit(");
                sb.append(info.byteOffset).append(", ").append(info.bitOffset).append(");");
            } else {
                sb.append("\n\t").append(info.typeName).append(" ").append(varName).append(" = io.");
                sb.append(info.dir == GvlContext.IODirection.INPUT ? "readInput<" : "readOutput<");
                sb.append(info.typeName).append(">(").append(info.byteOffset).append(");");
            }
            gvlCtx.locallyDeclaredGvlVars.add(varName);
        }
        return sb.toString();
    }

    /**
     * _post：自动保存 RETAIN 变量到非易失存储
     */
    private String emitPostRetainSave(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();
        ArrayList<PLCVariable> retainVars = new ArrayList<>();

        for (PLCSTPARSERParser.Other_var_declsContext other_var_decl : ctx.other_var_decls()) {
            ArrayList<PLCSymbol> otherVarDecl = PLCTranslatorNew.properties.get(other_var_decl);
            for (PLCSymbol symbol : otherVarDecl) {
                if (symbol instanceof PLCVariable varSymbol) {
                    if (varSymbol.getRetainQualifiers() == PLCModifierEnum.RetainModifier.RETAIN) {
                        retainVars.add(varSymbol);
                    }
                }
            }
        }

        if (!retainVars.isEmpty()) {
            sb.append("\n\tgvl.saveRetain();");
        }
        return sb.toString();
    }

    /**
     * cyclic 结尾：将所有局部变量写回 GVL
     */
    private String emitCyclicEpilogue(PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();
        GvlContext gvlCtx = translatorNew.gvlCtx;
        for (java.util.Map.Entry<String, Integer> entry : gvlCtx.offsetMap.entrySet()) {
            String varName = entry.getKey();
            int offset = entry.getValue();
            String type = gvlCtx.typeMap.get(varName);
            if (type == null) continue;
            if (gvlCtx.ioVarMap.containsKey(varName)) continue;
            if (type.startsWith("ARRAY[")) {
                int[][] bounds = gvlCtx.arrayBoundsMap.get(varName);
                String elemType = gvlCtx.arrayElemTypeMap.getOrDefault(varName, "INT");
                int totalCount = (bounds != null) ? bounds[0][2] : 0;
                if (totalCount > 0) {
                    int elemSize = gvlCtx.getTypeSize(elemType);
                    for (int i = 0; i < totalCount; i++) {
                        int elemOffset = offset + i * elemSize;
                        sb.append("\n\tgvl.write<").append(elemType).append(">(")
                          .append(elemOffset).append(", ").append(varName).append("[")
                          .append(i).append("]);");
                    }
                }
                continue;
            }
            sb.append("\n\tgvl.write<").append(type).append(">(").append(offset)
              .append(", ").append(varName).append(");");
        }
        for (java.util.Map.Entry<String, GvlContext.IOInfo> ioEntry : gvlCtx.ioVarMap.entrySet()) {
            String varName = ioEntry.getKey();
            GvlContext.IOInfo info = ioEntry.getValue();
            if (info.dir == GvlContext.IODirection.OUTPUT) {
                if (info.bitOffset >= 0) {
                    sb.append("\n\tio.writeOutputBit(").append(info.byteOffset)
                      .append(", ").append(info.bitOffset).append(", ").append(varName).append(");");
                } else {
                    sb.append("\n\tio.writeOutput<").append(info.typeName).append(">(")
                      .append(info.byteOffset).append(", ").append(varName).append(");");
                }
            }
        }
        return sb.toString();
    }
}
