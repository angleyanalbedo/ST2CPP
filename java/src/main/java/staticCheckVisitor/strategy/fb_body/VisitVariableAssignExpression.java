package staticCheckVisitor.strategy.fb_body;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.VisitorTool;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static antlr4.PLCSTPARSERParser.RULE_assign_stmt;

@StrategyForVisit(ruleIndex = RULE_assign_stmt, branch = 0)
public class VisitVariableAssignExpression implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.VariableAssignExpressionContext ctx = (PLCSTPARSERParser.VariableAssignExpressionContext) parserCtx;
        PLCVariable exprSymbol = (PLCVariable) visitor.visit(ctx.expression()).get(0);

        int exprTypeId = exprSymbol.getTypeId();
        PLCModifierEnum.Sort exprSort = exprSymbol.getSort();

        PLCVariable returnVar = (PLCVariable) visitor.visit(ctx.variable()).get(0);

        String varName = returnVar.getName();

        int varId = returnVar.getTypeId();
        PLCTypeDeclSymbol varSymbolType = PLCTotalSymbolTable.getTypeByTypeID(varId);

        if(!varSymbolType.checkCanAssignWith(exprTypeId)){
            throw new PLCSemanticException("type mismatch : " + ctx.getText());
        }

        returnVar.setAssignVar(exprSymbol.getAssignVar());
        return visitor.packSymbols(returnVar);
    }
}