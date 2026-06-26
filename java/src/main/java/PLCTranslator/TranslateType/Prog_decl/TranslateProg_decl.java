package PLCTranslator.TranslateType.Prog_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateProg_decl {

    public String translateNode(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();

        String progName = ctx.prog_type_name().identifier().getText();
        translatorNew.codeGen.addProgramName(progName);

        // ─── Init 回调：变量初始化 + RETAIN 区域 ───
        sb.append(translatorNew.codeGen.emitProgInitBegin(progName));

        // 翻译变量段（写入初始值）
        sb.append(emitVarInit(ctx, translatorNew));

        // 收集 RETAIN 变量并生成区域标记
        sb.append(emitRetainRegion(ctx, translatorNew));

        sb.append(translatorNew.codeGen.emitProgFuncEnd());

        // ─── Pre 回调（空） ───
        sb.append(translatorNew.codeGen.emitProgPreBegin(progName));
        sb.append(translatorNew.codeGen.emitProgFuncEnd());

        // ─── Cyclic 回调：循环体 ───
        sb.append(translatorNew.codeGen.emitProgCyclicBegin(progName));
        String result = translatorNew.visit(ctx.fb_body());
        sb.append(result);
        sb.append(translatorNew.codeGen.emitProgFuncEnd());

        // ─── Post 回调（空） ───
        sb.append(translatorNew.codeGen.emitProgPostBegin(progName));
        sb.append(translatorNew.codeGen.emitProgFuncEnd());

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
            // AT 地址变量：注册为 I/O 映射变量
            translatorNew.codeGen.registerIOVariable(
                varSymbol.getName(),
                varSymbol.getRuntimeTypeName(),
                location
            );
        } else {
            // 普通变量：分配 GVL 偏移量
            sb.append(translatorNew.codeGen.emitVarDecl(
                varSymbol.getName(),
                varSymbol.getRuntimeTypeName(),
                varSymbol.getAssignVar()
            ));
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
            java.util.Map<String, Integer> offsets = translatorNew.codeGen.getOffsetMap();
            java.util.Map<String, String> types = translatorNew.codeGen.getTypeMap();

            int retainStart = Integer.MAX_VALUE;
            int retainEnd = 0;
            for (PLCVariable rv : retainVars) {
                // 跳过 I/O 映射变量（无 GVL 偏移量）
                if (translatorNew.codeGen.isIOVariable(rv.getName())) continue;
                Integer off = offsets.get(rv.getName());
                if (off != null) {
                    retainStart = Math.min(retainStart, off);
                    String type = types.getOrDefault(rv.getName(), "INT");
                    int size = translatorNew.codeGen.getTypeSize(type);
                    retainEnd = Math.max(retainEnd, off + size);
                }
            }
            if (retainStart < Integer.MAX_VALUE && retainEnd > retainStart) {
                sb.append("\n\t\tgvl.setRetainRegion(").append(retainStart).append(", ").append(retainEnd).append(");");
            }
        }

        return sb.toString();
    }
}
