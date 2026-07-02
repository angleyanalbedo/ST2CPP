package PLCTranslator.TranslateType.Stmt.Assign_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import static PLCSymbolAndScope.PLCSymbols.PLCModifierEnum.Sort.FC_DECL;
import static PLCSymbolAndScope.PLCSymbols.PLCModifierEnum.Sort.METHOD_DECL;

public class TranslateVariableAssignExpression {
    public String translateNode(PLCSTPARSERParser.VariableAssignExpressionContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        PLCVariable varSymbol = PLCTranslatorNew.getVariable(ctx, "assign target");

        // 1. 通过 visitor 翻译右侧表达式（函数调用会产生 pendingDecls）
        String rhs = translatorNew.visit(ctx.expression());

        // 2. flush pendingDecls（临时变量声明）
        for (String decl : translatorNew.pendingDecls) {
            sb.append(decl);
        }
        translatorNew.pendingDecls.clear();

        // 3. 判断是否在 FC/METHOD 内部（通过 localScope 的 declSymbol）
        boolean inFC = false;
        if (varSymbol.getLocalScope() != null && varSymbol.getLocalScope().getDeclSymbol() != null) {
            PLCModifierEnum.Sort scopeSort = varSymbol.getLocalScope().getDeclSymbol().getSort();
            inFC = (scopeSort == FC_DECL || scopeSort == METHOD_DECL);
        }

        if (!inFC) {
            String varName = varSymbol.getName();
            if (varName.startsWith("*")) varName = varName.substring(1);
            if (varName.startsWith("(") && varName.endsWith(")")) {
                varName = varName.substring(1, varName.length() - 1);
            }

            if (translatorNew.gvlCtx.isIOVariable(varName)) {
                // I/O 映射变量 → io.writeOutput
                String ioWrite = translatorNew.gvlCtx.emitIOWrite(varName, rhs);
                if (ioWrite != null) {
                    sb.append("\n\t\t").append(ioWrite).append(";");
                } else {
                    sb.append("\n\t\t").append(varName).append(" = ").append(rhs).append(";");
                }
            } else {
                // Handle array subscript (name[...]) and struct member (name.field):
                // extract base GVL variable name before first separator
                String baseName = varName.replaceAll("[\\[.].*", "");
                if (translatorNew.gvlCtx.typeMap.containsKey(baseName)) {
                    String mangledBase = "gv." + translatorNew.gvlCtx.getMangledName(baseName);
                    if (varName.equals(baseName)) {
                        sb.append("\n\t\t").append(mangledBase).append(" = ").append(rhs).append(";");
                    } else {
                        String suffix = varName.substring(baseName.length());
                        // Only translate index expressions inside brackets, not struct field names
                        if (suffix.contains("[")) {
                            suffix = translatorNew.gvlCtx.translateExpr(suffix);
                        }
                        sb.append("\n\t\t").append(mangledBase)
                          .append(suffix)
                          .append(" = ").append(rhs).append(";");
                    }
                } else {
                    // 局部变量或参数
                    sb.append("\n\t\t").append(varName).append(" = ").append(rhs).append(";");
                }
            }
        }else{
            // 在 FC/METHOD 内部
            String varName = varSymbol.getName();
            // 语义分析器把函数返回赋值（如 WRAP_ANGLE := x）解析为 *this->returnValue
            if (varName.contains("this->returnValue")) {
                sb.append("\n\t\treturn ").append(rhs).append(";");
            } else {
                // 局部变量或参数，正常赋值
                if (varName.startsWith("*")) varName = varName.substring(1);
                sb.append("\n\t\t").append(varName).append(" = ").append(rhs).append(";");
            }
        }
        return sb.toString();
    }
}
