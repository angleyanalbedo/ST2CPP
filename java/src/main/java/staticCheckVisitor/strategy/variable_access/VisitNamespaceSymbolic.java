package staticCheckVisitor.strategy.variable_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static antlr4.PLCSTPARSERParser.RULE_symbolic_variable;

/**
 * 处理 namespaceSymbolic 分支（branch=1 of symbolic_variable）：
 *   ( namespace_name '.' )* ( var_access | multi_elem_var )
 *
 * multi_elem_var 支持交替的数组索引和 struct 字段访问：
 *   var_access ( subscript_list | struct_variable )+
 *
 * 例如：ARR_OF_STRUCT[I].FIELD
 */
@StrategyForVisit(ruleIndex = RULE_symbolic_variable, branch = 1)
public class VisitNamespaceSymbolic implements Strategy {

    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.NamespaceSymbolicContext ctx =
                (PLCSTPARSERParser.NamespaceSymbolicContext) parserCtx;

        PLCVariable resultVar;

        if (ctx.multi_elem_var() != null) {
            resultVar = visitMultiElemVar(ctx.multi_elem_var(), visitor);
        } else if (ctx.var_access() != null) {
            resultVar = visitSimpleVarAccess(ctx.var_access(), visitor);
        } else {
            throw new PLCSemanticException("empty namespaceSymbolic: " + ctx.getText());
        }

        return visitor.packSymbols(resultVar);
    }

    private PLCVariable visitSimpleVarAccess(
            PLCSTPARSERParser.Var_accessContext varAccessCtx, PLCVisitor visitor) {
        String varName = varAccessCtx.variable_name().identifier().getText();
        PLCSymbol symbol = currentScope.deepFindSymbol(varName);
        if (!(symbol instanceof PLCVariable varSymbol)) {
            throw new PLCSemanticException("variable not found: " + varName + " FROM : " + varAccessCtx.getText());
        }
        PLCVariable result = new PLCVariable(varSymbol);
        result.setName(symbol.getLocalScope() == currentScope ? "*" + varName : varSymbol.getUniqueName());
        return result;
    }

    private PLCVariable visitMultiElemVar(
            PLCSTPARSERParser.Multi_elem_varContext multiCtx, PLCVisitor visitor) {

        // 1. 获取基础变量
        String baseName = multiCtx.var_access().variable_name().identifier().getText();
        PLCSymbol baseSymbol = currentScope.deepFindSymbol(baseName);
        if (!(baseSymbol instanceof PLCVariable baseVar)) {
            throw new PLCSemanticException("variable not found: " + baseName + " FROM : " + multiCtx.getText());
        }

        PLCVariable result = new PLCVariable(baseVar);
        String currentName = baseSymbol.getLocalScope() == currentScope ? "*" + baseName : baseVar.getUniqueName();
        result.setName(currentName);

        // 构建 assignVar 路径用的名字（不含 *)
        String pathName = currentName.startsWith("*") ? currentName.substring(1) : currentName;

        // 2. 遍历交替的 subscript_list / struct_variable
        // 按顺序处理（multi_elem_var 的 children 按 grammar 顺序排列）
        // children: [0]=var_access, [1..N]=subscript_list/struct_variable 交替
        for (int i = 1; i < multiCtx.getChildCount(); i++) {
            ParseTree child = multiCtx.getChild(i);
            if (child instanceof PLCSTPARSERParser.Subscript_listContext) {
                PLCSTPARSERParser.Subscript_listContext subCtx =
                    (PLCSTPARSERParser.Subscript_listContext) child;
                // 数组索引访问
                int currentTypeId = result.getTypeId();
                PLCTypeDeclSymbol typeSymbol = PLCTotalSymbolTable.getTypeByTypeID(currentTypeId);
                if (!(typeSymbol instanceof PLCArrayDeclSymbol)) {
                    throw new PLCSemanticException("type " + typeSymbol.getName()
                            + " is not subscriptable: " + multiCtx.getText());
                }
                PLCArrayDeclSymbol arrayType = (PLCArrayDeclSymbol) typeSymbol;
                // 更新类型为数组元素类型
                result.setTypeId(arrayType.getElementTypeId());

                // 获取索引表达式文本
                String indexExpr = subCtx.subscript(0).expression().getText();
                // 去掉当前 name 中的前导 * 和外部括号
                String cleanPath = currentName.startsWith("*") ? currentName.substring(1) : currentName;
                if (cleanPath.startsWith("(") && cleanPath.endsWith(")")) {
                    cleanPath = cleanPath.substring(1, cleanPath.length() - 1);
                }
                currentName = "*(" + cleanPath + "[" + indexExpr + "])";
                pathName = "(" + cleanPath + "[" + indexExpr + "])";
                result.setName(currentName);
                if (result instanceof PLCVariable) {
                    ((PLCVariable) result).setAssignVar(pathName);
                }

            } else if (child instanceof PLCSTPARSERParser.Struct_variableContext) {
                PLCSTPARSERParser.Struct_variableContext structCtx =
                    (PLCSTPARSERParser.Struct_variableContext) child;
                // struct 字段访问
                String fieldName = structCtx.struct_elem_select()
                        .var_access().variable_name().identifier().getText();

                int currentTypeId = result.getTypeId();
                PLCTypeDeclSymbol typeSymbol = PLCTotalSymbolTable.getTypeByTypeID(currentTypeId);
                if (!(typeSymbol instanceof PLCStructDeclSymbol)) {
                    throw new PLCSemanticException("variable " + result.getName()
                            + " is not a struct type FROM : " + multiCtx.getText());
                }
                PLCStructDeclSymbol structType = (PLCStructDeclSymbol) typeSymbol;

                // 查找字段
                PLCVariable fieldVar = null;
                for (PLCVariable fv : structType.getVariables()) {
                    if (fv.getName().equals(fieldName)) {
                        fieldVar = fv;
                        break;
                    }
                }
                if (fieldVar == null) {
                    throw new PLCSemanticException("struct " + typeSymbol.getName()
                            + " has no member named " + fieldName + " FROM : " + multiCtx.getText());
                }

                // 更新类型信息
                result.setTypeId(fieldVar.getTypeId());
                result.setSort(fieldVar.getSort());
                result.setRuntimeTypeName(fieldVar.getRuntimeTypeName());

                // 构造带字段的路径
                String cleanPath = currentName.startsWith("*") ? currentName.substring(1) : currentName;
                if (cleanPath.startsWith("(") && cleanPath.endsWith(")")) {
                    cleanPath = cleanPath.substring(1, cleanPath.length() - 1);
                }
                currentName = "*(" + cleanPath + "." + fieldName + ")";
                pathName = "(" + cleanPath + "." + fieldName + ")";
                result.setName(currentName);
                if (result instanceof PLCVariable) {
                    PLCVariable rv = (PLCVariable) result;
                    rv.setAssignVar(pathName);
                    rv.setDeclSymbol(fieldVar.getDeclSymbol());
                }
            }
        }

        return result;
    }
}
