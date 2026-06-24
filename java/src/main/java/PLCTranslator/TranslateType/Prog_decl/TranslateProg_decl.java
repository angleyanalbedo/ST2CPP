package PLCTranslator.TranslateType.Prog_decl;

import PLCSymbolAndScope.PLCSymbols.PLCArrayDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCEnumDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTranslator.TranslateType.Startpoint.TranslateStartpoint.funcInitSentences;

public class TranslateProg_decl {

    packageFactory pFactory = new packageFactory();

    public String translateNode(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        boolean isFlat = translatorNew.codeGen instanceof FlatCodeGenerator;
        StringBuilder sb = new StringBuilder();

        if (isFlat) {
            // Flat 模式：生成 void PROGRAM_Name(GVL& gvl, ProcessImage& io, TIME dt) { ... }
            String progName = ctx.prog_type_name().identifier().getText();
            sb.append(translatorNew.codeGen.emitProgDeclBegin(progName));
        } else {
            // OOP 模式：保持原有逻辑
            //********************************************全局静态函数初始化***************************************************
            sb.append("\nvoid initFunc(){");

            for(String funcInitSentence : funcInitSentences){
                sb.append("\n\t"+funcInitSentence);
            }
            sb.append("\n}\n");

            //*********************************************main函数翻译******************************************************
            sb.append("\n"+"int main(){");
            //翻译静态方法
            sb.append("\n\tinitFunc();");
        }

        //翻译变量段
        for (PLCSTPARSERParser.Func_var_declsContext func_var_decl : ctx.func_var_decls()) {
            ArrayList<PLCSymbol> funcVarDecl = PLCTranslatorNew.properties.get(func_var_decl);
            //翻译每一条变量声明定义语句
            for (PLCSymbol symbol : funcVarDecl) {
                if(symbol instanceof PLCEnumDeclSymbol enumDeclSymbol){
                    if (isFlat) {
                        sb.append("\n\t// Flat: TODO enum declaration");
                    }
                } else if (symbol instanceof PLCArrayDeclSymbol varSymbol) {
                    if (isFlat) {
                        sb.append("\n\t// Flat: TODO array declaration");
                    }
                }else{
                    PLCVariable varSymbol = (PLCVariable) symbol;
                    if (isFlat) {
                        // Flat 模式：通过 codeGen.emitVarDecl 分配 GVL 偏移量
                        sb.append(translatorNew.codeGen.emitVarDecl(varSymbol.getName(), varSymbol.getRuntimeTypeName(), varSymbol.getAssignVar()));
                    } else {
                        // OOP 模式：保持原有逻辑
                        sb.append("\n\t"+pFactory.packagePROGVarDeclSentences(symbol.getName(), varSymbol.getRuntimeTypeName(),
                                varSymbol.getAssignVar()));
                    }
                }
            }

        }

        for (PLCSTPARSERParser.Temp_var_declsContext temp_var_decl : ctx.temp_var_decls()) {
            ArrayList<PLCSymbol> tempVarDecl = PLCTranslatorNew.properties.get(temp_var_decl);

            for (PLCSymbol symbol : tempVarDecl) {
                if(symbol instanceof PLCEnumDeclSymbol enumDeclSymbol){
                    if (isFlat) {
                        sb.append("\n\t// Flat: TODO enum declaration");
                    }
                } else if (symbol instanceof PLCArrayDeclSymbol varSymbol) {
                    if (isFlat) {
                        sb.append("\n\t// Flat: TODO array declaration");
                    }
                }else{
                    PLCVariable varSymbol = (PLCVariable) symbol;
                    if (isFlat) {
                        sb.append(translatorNew.codeGen.emitVarDecl(varSymbol.getName(), varSymbol.getRuntimeTypeName(), varSymbol.getAssignVar()));
                    } else {
                        sb.append("\n\t"+pFactory.packagePROGVarDeclSentences(symbol.getRuntimeName(), varSymbol.getRuntimeTypeName(),
                                varSymbol.getAssignVar()));
                    }
                }
            }

        }

        // 收集 RETAIN 变量（Flat 模式）
        java.util.ArrayList<PLCVariable> retainVars = new java.util.ArrayList<>();

        for (PLCSTPARSERParser.Other_var_declsContext other_var_decl : ctx.other_var_decls()) {
            ArrayList<PLCSymbol> otherVarDecl = PLCTranslatorNew.properties.get(other_var_decl);

            for (PLCSymbol symbol : otherVarDecl) {
                if(symbol instanceof PLCEnumDeclSymbol enumDeclSymbol){
                    if (isFlat) {
                        sb.append("\n\t// Flat: TODO enum declaration");
                    }
                } else if (symbol instanceof PLCArrayDeclSymbol varSymbol) {
                    if (isFlat) {
                        sb.append("\n\t// Flat: TODO array declaration");
                    }
                }else{
                    PLCVariable varSymbol = (PLCVariable) symbol;
                    if (isFlat) {
                        sb.append(translatorNew.codeGen.emitVarDecl(varSymbol.getName(), varSymbol.getRuntimeTypeName(), varSymbol.getAssignVar()));
                        // 收集 RETAIN 变量
                        if (varSymbol.getRetainQualifiers() == PLCModifierEnum.RetainModifier.RETAIN) {
                            retainVars.add(varSymbol);
                        }
                    } else {
                        sb.append("\n\t"+pFactory.packagePROGVarDeclSentences(symbol.getRuntimeName(), varSymbol.getRuntimeTypeName(),
                                varSymbol.getAssignVar()));
                    }
                }
            }
        }

        // Flat 模式：生成 RETAIN 区域标记
        if (isFlat && !retainVars.isEmpty()) {
            // 获取第一个和最后一个 RETAIN 变量的偏移量
            FlatCodeGenerator flatGen = (FlatCodeGenerator) translatorNew.codeGen;
            java.util.Map<String, Integer> offsets = flatGen.getOffsetMap();
            java.util.Map<String, String> types = flatGen.getTypeMap();

            int retainStart = Integer.MAX_VALUE;
            int retainEnd = 0;
            for (PLCVariable rv : retainVars) {
                Integer off = offsets.get(rv.getName());
                if (off != null) {
                    retainStart = Math.min(retainStart, off);
                    // 计算变量结束位置（偏移量 + 类型大小）
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
        if (isFlat) {
            sb.append(translatorNew.codeGen.emitProgDeclEnd());
        } else {
            sb.append("\n}");
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
