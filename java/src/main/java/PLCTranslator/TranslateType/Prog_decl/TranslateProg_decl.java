package PLCTranslator.TranslateType.Prog_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateProg_decl {

    public String translateNode(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();

        // 生成 void PROGRAM_Name(GVL& gvl, ProcessImage& io, TIME dt) { ... }
        String progName = ctx.prog_type_name().identifier().getText();
        sb.append(translatorNew.codeGen.emitProgDeclBegin(progName));

        //翻译变量段
        for (PLCSTPARSERParser.Func_var_declsContext func_var_decl : ctx.func_var_decls()) {
            ArrayList<PLCSymbol> funcVarDecl = PLCTranslatorNew.properties.get(func_var_decl);
            for (PLCSymbol symbol : funcVarDecl) {
                if(symbol instanceof PLCEnumDeclSymbol enumDeclSymbol){
                    sb.append("\n\t// TODO: enum declaration");
                } else if (symbol instanceof PLCArrayDeclSymbol varSymbol) {
                    sb.append("\n\t// TODO: array declaration");
                }else{
                    PLCVariable varSymbol = (PLCVariable) symbol;
                    sb.append(translatorNew.codeGen.emitVarDecl(varSymbol.getName(), varSymbol.getRuntimeTypeName(), varSymbol.getAssignVar()));
                }
            }
        }

        for (PLCSTPARSERParser.Temp_var_declsContext temp_var_decl : ctx.temp_var_decls()) {
            ArrayList<PLCSymbol> tempVarDecl = PLCTranslatorNew.properties.get(temp_var_decl);
            for (PLCSymbol symbol : tempVarDecl) {
                if(symbol instanceof PLCEnumDeclSymbol enumDeclSymbol){
                    sb.append("\n\t// TODO: enum declaration");
                } else if (symbol instanceof PLCArrayDeclSymbol varSymbol) {
                    sb.append("\n\t// TODO: array declaration");
                }else{
                    PLCVariable varSymbol = (PLCVariable) symbol;
                    sb.append(translatorNew.codeGen.emitVarDecl(varSymbol.getName(), varSymbol.getRuntimeTypeName(), varSymbol.getAssignVar()));
                }
            }
        }

        // 收集 RETAIN 变量
        java.util.ArrayList<PLCVariable> retainVars = new java.util.ArrayList<>();

        for (PLCSTPARSERParser.Other_var_declsContext other_var_decl : ctx.other_var_decls()) {
            ArrayList<PLCSymbol> otherVarDecl = PLCTranslatorNew.properties.get(other_var_decl);
            for (PLCSymbol symbol : otherVarDecl) {
                if(symbol instanceof PLCEnumDeclSymbol enumDeclSymbol){
                    sb.append("\n\t// TODO: enum declaration");
                } else if (symbol instanceof PLCArrayDeclSymbol varSymbol) {
                    sb.append("\n\t// TODO: array declaration");
                }else{
                    PLCVariable varSymbol = (PLCVariable) symbol;
                    sb.append(translatorNew.codeGen.emitVarDecl(varSymbol.getName(), varSymbol.getRuntimeTypeName(), varSymbol.getAssignVar()));
                    // 收集 RETAIN 变量
                    if (varSymbol.getRetainQualifiers() == PLCModifierEnum.RetainModifier.RETAIN) {
                        retainVars.add(varSymbol);
                    }
                }
            }
        }

        // 生成 RETAIN 区域标记
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
                int retainLength = retainEnd - retainStart;
                sb.append("\n\t\tgvl.setRetainRegion(").append(retainStart).append(", ").append(retainLength).append(");");
            }
        }

        String result = translatorNew.visit(ctx.fb_body());
        sb.append(result);
        sb.append(translatorNew.codeGen.emitProgDeclEnd());
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
