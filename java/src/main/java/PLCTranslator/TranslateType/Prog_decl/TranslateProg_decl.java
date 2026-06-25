package PLCTranslator.TranslateType.Prog_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateProg_decl {

    public String translateNode(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();

        String progName = ctx.prog_type_name().identifier().getText();
        if (translatorNew.codeGen instanceof FlatCodeGenerator) {
            ((FlatCodeGenerator)translatorNew.codeGen).addProgramName(progName);
        }

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
     * 生成变量初始化语句（GVL 写入）
     */
    private String emitVarInit(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();

        for (PLCSTPARSERParser.Func_var_declsContext func_var_decl : ctx.func_var_decls()) {
            ArrayList<PLCSymbol> funcVarDecl = PLCTranslatorNew.properties.get(func_var_decl);
            for (PLCSymbol symbol : funcVarDecl) {
                if (symbol instanceof PLCVariable varSymbol) {
                    sb.append(translatorNew.codeGen.emitVarDecl(varSymbol.getName(), varSymbol.getRuntimeTypeName(), varSymbol.getAssignVar()));
                }
            }
        }

        for (PLCSTPARSERParser.Temp_var_declsContext temp_var_decl : ctx.temp_var_decls()) {
            ArrayList<PLCSymbol> tempVarDecl = PLCTranslatorNew.properties.get(temp_var_decl);
            for (PLCSymbol symbol : tempVarDecl) {
                if (symbol instanceof PLCVariable varSymbol) {
                    sb.append(translatorNew.codeGen.emitVarDecl(varSymbol.getName(), varSymbol.getRuntimeTypeName(), varSymbol.getAssignVar()));
                }
            }
        }

        for (PLCSTPARSERParser.Other_var_declsContext other_var_decl : ctx.other_var_decls()) {
            ArrayList<PLCSymbol> otherVarDecl = PLCTranslatorNew.properties.get(other_var_decl);
            for (PLCSymbol symbol : otherVarDecl) {
                if (symbol instanceof PLCVariable varSymbol) {
                    sb.append(translatorNew.codeGen.emitVarDecl(varSymbol.getName(), varSymbol.getRuntimeTypeName(), varSymbol.getAssignVar()));
                }
            }
        }

        return sb.toString();
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
            FlatCodeGenerator flatGen = (FlatCodeGenerator) translatorNew.codeGen;
            java.util.Map<String, Integer> offsets = flatGen.getOffsetMap();
            java.util.Map<String, String> types = flatGen.getTypeMap();

            int retainStart = Integer.MAX_VALUE;
            int retainEnd = 0;
            for (PLCVariable rv : retainVars) {
                Integer off = offsets.get(rv.getName());
                if (off != null) {
                    retainStart = Math.min(retainStart, off);
                    String type = types.getOrDefault(rv.getName(), "INT");
                    int size = getTypeSize(type);
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
     * 获取类型的字节大小（用于 RETAIN 区域计算）
     */
    private int getTypeSize(String type) {
        switch (type) {
            case "SINT": case "USINT": case "BOOL": return 1;
            case "INT": case "UINT": return 2;
            case "DINT": case "UDINT": case "REAL": return 4;
            case "LINT": case "ULINT": case "LREAL": case "TIME": return 8;
            case "STRING": return 256;
            default: return 4;
        }
    }
}
