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
import java.util.List;

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static antlr4.PLCSTPARSERParser.RULE_symbolic_variable;

@StrategyForVisit(ruleIndex = RULE_symbolic_variable, branch = 1)
public class VisitNamespaceSymbolic implements Strategy {

    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.NamespaceSymbolicContext ctx =
                (PLCSTPARSERParser.NamespaceSymbolicContext) parserCtx;

        PLCVariable resultVar;

        if (ctx.multi_elem_var() != null) {
            List<String> nsNames = new ArrayList<>();
            for (var nc : ctx.namespace_name()) {
                nsNames.add(nc.getText());
            }
            if (!nsNames.isEmpty()) {
                PLCSymbol firstSym = currentScope.deepFindSymbol(nsNames.get(0));
                if (firstSym instanceof PLCVariable baseVar) {
                    resultVar = visitMultiElemVarWithNamespace(
                            ctx.multi_elem_var(), nsNames, baseVar, visitor);
                    return visitor.packSymbols(resultVar);
                }
            }
            resultVar = visitMultiElemVar(ctx.multi_elem_var(), visitor);
        } else if (ctx.var_access() != null) {
            resultVar = visitSimpleVarAccess(ctx.var_access());
        } else {
            throw new PLCSemanticException("empty namespaceSymbolic: " + ctx.getText());
        }

        return visitor.packSymbols(resultVar);
    }

    private PLCVariable visitSimpleVarAccess(
            PLCSTPARSERParser.Var_accessContext varAccessCtx) {
        String varName = varAccessCtx.variable_name().identifier().getText();
        PLCSymbol symbol = currentScope.deepFindSymbol(varName);
        if (!(symbol instanceof PLCVariable varSymbol)) {
            throw new PLCSemanticException("variable not found: " + varName + " FROM : " + varAccessCtx.getText());
        }
        PLCVariable result = new PLCVariable(varSymbol);
        result.setName(symbol.getLocalScope() == currentScope ? "*" + varName : varSymbol.getUniqueName());
        return result;
    }

    private PLCVariable visitMultiElemVarWithNamespace(
            PLCSTPARSERParser.Multi_elem_varContext multiCtx,
            List<String> nsNames,
            PLCVariable baseVar,
            PLCVisitor visitor) {

        PLCVariable result = new PLCVariable(baseVar);
        String currentName = baseVar.getLocalScope() == currentScope
                ? "*" + nsNames.get(0) : baseVar.getUniqueName();
        result.setName(currentName);

        for (int j = 1; j < nsNames.size(); j++) {
            String fieldName = nsNames.get(j);
            result = resolveStructField(result, fieldName, multiCtx);
            String cleanPath = cleanPath(currentName);
            currentName = "*(" + cleanPath + "." + fieldName + ")";
            result.setName(currentName);
            result.setAssignVar("(" + cleanPath + "." + fieldName + ")");
        }

        result = processMultiElemChildren(result, multiCtx, 0, visitor);
        return result;
    }

    private PLCVariable visitMultiElemVar(
            PLCSTPARSERParser.Multi_elem_varContext multiCtx,
            PLCVisitor visitor) {

        String baseName = multiCtx.var_access().variable_name().identifier().getText();
        PLCSymbol baseSymbol = currentScope.deepFindSymbol(baseName);
        if (!(baseSymbol instanceof PLCVariable baseVar)) {
            throw new PLCSemanticException("variable not found: " + baseName + " FROM : " + multiCtx.getText());
        }

        PLCVariable result = new PLCVariable(baseVar);
        result.setName(baseSymbol.getLocalScope() == currentScope
                ? "*" + baseName : baseVar.getUniqueName());

        result = processMultiElemChildren(result, multiCtx, 1, visitor);
        return result;
    }

    private PLCVariable processMultiElemChildren(
            PLCVariable result,
            PLCSTPARSERParser.Multi_elem_varContext multiCtx,
            int startIndex,
            PLCVisitor visitor) {

        String currentName = result.getName();

        for (int i = startIndex; i < multiCtx.getChildCount(); i++) {
            ParseTree child = multiCtx.getChild(i);

            if (child instanceof PLCSTPARSERParser.Subscript_listContext) {
                PLCSTPARSERParser.Subscript_listContext subCtx =
                        (PLCSTPARSERParser.Subscript_listContext) child;

                int currentTypeId = result.getTypeId();
                PLCTypeDeclSymbol typeSymbol = PLCTotalSymbolTable.getTypeByTypeID(currentTypeId);
                if (!(typeSymbol instanceof PLCArrayDeclSymbol)) {
                    throw new PLCSemanticException("type " + typeSymbol.getName()
                            + " is not subscriptable: " + multiCtx.getText());
                }
                PLCArrayDeclSymbol arrayType = (PLCArrayDeclSymbol) typeSymbol;
                result.setTypeId(arrayType.getElementTypeId());

                // ST 多维下标 [i,j] → C++ [i][j]
                // 访问表达式以验证符号存在性
                StringBuilder idxBuilder = new StringBuilder();
                for (int s = 0; s < subCtx.subscript().size(); s++) {
                    if (s > 0) idxBuilder.append("][");
                    ArrayList<PLCSymbol> exprSyms = visitor.visit(
                            subCtx.subscript(s).expression());
                    String exprText = null;
                    if (exprSyms != null && !exprSyms.isEmpty()
                            && exprSyms.get(0) instanceof PLCVariable ev) {
                        // 使用 name（变量名）而非 assignVar（初始化值/常量折叠结果）
                        exprText = ev.getName();
                        if (exprText == null || exprText.isEmpty()) {
                            exprText = ev.getAssignVar();
                        }
                        if (exprText != null && exprText.startsWith("*")) {
                            exprText = exprText.substring(1);
                        }
                        if (exprText != null && exprText.startsWith("(") && exprText.endsWith(")")) {
                            exprText = exprText.substring(1, exprText.length() - 1);
                        }
                    }
                    if (exprText == null || exprText.isEmpty()) {
                        exprText = subCtx.subscript(s).expression().getText();
                    }
                    idxBuilder.append(exprText);
                }
                String indexExpr = idxBuilder.toString();
                String cleanPath = cleanPath(currentName);
                currentName = "*(" + cleanPath + "[" + indexExpr + "])";
                result.setName(currentName);
                result.setAssignVar("(" + cleanPath + "[" + indexExpr + "])");

            } else if (child instanceof PLCSTPARSERParser.Struct_variableContext) {
                PLCSTPARSERParser.Struct_variableContext structCtx =
                        (PLCSTPARSERParser.Struct_variableContext) child;
                String fieldName = structCtx.struct_elem_select()
                        .var_access().variable_name().identifier().getText();

                result = resolveStructField(result, fieldName, multiCtx);

                String cleanPath = cleanPath(currentName);
                currentName = "*(" + cleanPath + "." + fieldName + ")";
                result.setName(currentName);
                result.setAssignVar("(" + cleanPath + "." + fieldName + ")");

            } else if (i == 0 && child instanceof PLCSTPARSERParser.Var_accessContext) {
                String fieldName = ((PLCSTPARSERParser.Var_accessContext) child)
                        .variable_name().identifier().getText();

                result = resolveStructField(result, fieldName, multiCtx);

                String cleanPath = cleanPath(currentName);
                currentName = "*(" + cleanPath + "." + fieldName + ")";
                result.setName(currentName);
                result.setAssignVar("(" + cleanPath + "." + fieldName + ")");
            }
        }

        return result;
    }

    private static PLCVariable resolveStructField(PLCVariable current, String fieldName, ParserRuleContext errorCtx) {
        int currentTypeId = current.getTypeId();
        PLCTypeDeclSymbol typeSymbol = PLCTotalSymbolTable.getTypeByTypeID(currentTypeId);

        PLCVariable fieldVar = null;

        if (typeSymbol instanceof PLCStructDeclSymbol structType) {
            for (PLCVariable fv : structType.getVariables()) {
                if (fv.getName().equals(fieldName)) {
                    fieldVar = fv;
                    break;
                }
            }
            if (fieldVar == null) {
                throw new PLCSemanticException("struct " + typeSymbol.getName()
                        + " has no member named " + fieldName + " FROM : " + errorCtx.getText(), errorCtx);
            }
        } else if (typeSymbol instanceof PLCBaseClassDeclSymbol classType) {
            PLCSymbolTable importTable = classType.getImportSymbolTable();
            PLCSymbol fieldSym = (importTable != null) ? importTable.findSymbol(fieldName) : null;
            fieldVar = (fieldSym instanceof PLCVariable) ? (PLCVariable) fieldSym : null;
            if (fieldVar == null) {
                throw new PLCSemanticException("type " + typeSymbol.getName()
                        + " has no member named " + fieldName + " FROM : " + errorCtx.getText(), errorCtx);
            }
        } else if (typeSymbol == null) {
            throw new PLCSemanticException("variable " + current.getName()
                    + " has unknown type (typeId=" + currentTypeId + ") FROM : " + errorCtx.getText(), errorCtx);
        } else {
            throw new PLCSemanticException("variable " + current.getName()
                    + " is not a struct or FB type FROM : " + errorCtx.getText(), errorCtx);
        }

        PLCVariable updated = new PLCVariable(current);
        updated.setTypeId(fieldVar.getTypeId());
        updated.setSort(fieldVar.getSort());
        updated.setRuntimeTypeName(fieldVar.getRuntimeTypeName());
        updated.setDeclSymbol(fieldVar.getDeclSymbol());
        return updated;
    }

    private static String cleanPath(String name) {
        String cp = name.startsWith("*") ? name.substring(1) : name;
        if (cp.startsWith("(") && cp.endsWith(")")) {
            cp = cp.substring(1, cp.length() - 1);
        }
        return cp;
    }
}
