package PLCTranslator.TranslateType.Prog_decl;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class TranslateProg_decl {

    public String translateNode(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();

        // gvl_layout.gen.h — 在 PROGRAM 函数之前引入（所有 FB 类型已定义）
        sb.append(translatorNew.gvlCtx.emitGVLLayoutIncludeOnce());

        String progName = ctx.prog_type_name().identifier().getText();
        translatorNew.gvlCtx.addProgramName(progName);
        // 记录当前 PROGRAM，后续 allocateOffset 会据此生成 mangled 字段名
        translatorNew.gvlCtx.setCurrentProgram(progName);

        String mangled = translatorNew.gvlCtx.mangleProgName(progName);

        sb.append("\nvoid PROGRAM_").append(mangled).append("_init(GVL& gvl, ProcessImage& io) {");
        sb.append("\n\tauto& gv = rt_plc::gvl_layout(gvl);");
        sb.append(emitVarInit(ctx, translatorNew));
        sb.append(emitRetainRegion(ctx, translatorNew));
        sb.append(emitInitRetainLoad(ctx, translatorNew));
        sb.append("\n}");

        sb.append("\nvoid PROGRAM_").append(mangled).append("_pre(GVL& gvl, ProcessImage& io) {");
        sb.append("\n}");

        sb.append("\nvoid PROGRAM_").append(mangled).append("_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {");
        sb.append("\n\tauto& gv = rt_plc::gvl_layout(gvl);");
        translatorNew.inCyclic = true;
        String result = translatorNew.visit(ctx.fb_body());
        translatorNew.inCyclic = false;
        sb.append(result);
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
            GvlContext gvlCtx = translatorNew.gvlCtx;
            PLCVariable first = null;
            PLCVariable last = null;
            for (PLCVariable rv : retainVars) {
                if (gvlCtx.isIOVariable(rv.getName())) continue;
                if (first == null) first = rv;
                last = rv;
            }
            if (first != null && last != null) {
                String firstM = gvlCtx.getMangledName(first.getName());
                String lastM = gvlCtx.getMangledName(last.getName());
                String lastType = gvlCtx.typeMap.get(last.getName());
                if (lastType == null) lastType = "INT";
                sb.append("\n\t\tgvl.setRetainRegion(");
                sb.append("(uint8_t*)&gv.").append(firstM).append(" - gvl.memory, ");
                sb.append("(uint8_t*)&gv.").append(lastM).append(" - gvl.memory + sizeof(").append(lastType).append(")");
                sb.append(");");
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
            gvlCtx.allocateArrayOffset(name, varSymbol.getArrayTotalCount(), elemTypeNative, arrayBounds);
            return;
        }

        // 枚举变量不分配 GVL 偏移
        if (typeId != 0) {
            PLCTypeDeclSymbol typeDecl = PLCTotalSymbolTable.getTypeByTypeID(typeId);
            if (typeDecl instanceof PLCEnumDeclSymbol) {
                return;
            }
        }

        String nativeType = gvlCtx.toNativeType(varSymbol.getRuntimeTypeName());
        gvlCtx.allocateOffset(name, nativeType);
        String initValue = resolveInitValue(varSymbol);
        if (initValue != null && !initValue.isEmpty() && !"0".equals(initValue) && !"\"\"".equals(initValue)) {
            String mangled = gvlCtx.getMangledName(name);
            sb.append("\n\t\tgv.").append(mangled).append(" = ").append(initValue).append(";");
        }
    }

    /**
     * 从 PLCVariable 解析初始值：
     * 优先使用结构化 namedInit，退化到 assignVar 字符串解析。
     * 输出格式：{val1, val2, ...}（C++ 聚合初始化）
     */
    private String resolveInitValue(PLCVariable varSymbol) {
        // 优先：结构化 namedInit（来自 grammar struct_init）
        if (varSymbol.getInitKind() == PLCVariable.InitKind.AGGREGATE && varSymbol.getNamedInit() != null) {
            LinkedHashMap<String, String> named = varSymbol.getNamedInit();
            if (!named.isEmpty()) {
                StringBuilder sb = new StringBuilder("{");
                boolean first = true;
                for (java.util.Map.Entry<String, String> entry : named.entrySet()) {
                    if (!first) sb.append(", ");
                    sb.append(entry.getValue());
                    first = false;
                }
                sb.append("}");
                return sb.toString();
            }
        }
        // 退化：字符串解析
        String assignVar = varSymbol.getAssignVar();
        if (assignVar == null || assignVar.isEmpty()) return null;
        if (assignVar.contains(":=")) {
            return toAggregateInit(assignVar);
        }
        return PLCVariable.stripParens(assignVar);
    }

    /**
     * 将 ST 命名参数初始化转为 C++ 聚合初始化（字符串后备）。
     * 例: (KP:=2.0,KI:=0.5) → {2.0, 0.5}
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

}
