package staticCheckVisitor.strategy.constant_expr;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.VisitorTool;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;
import static antlr4.PLCSTPARSERParser.RULE_unary_expr;

@StrategyForVisit(ruleIndex = RULE_unary_expr)
public class VisitUnary_expr implements Strategy {
    /**
     * 三种情况：
     * 1. NOT ---primary必须为bool
     * 2. + / -  -----primary必须为calc
     * 3. 无 ------primary不规定
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Unary_exprContext ctx = (PLCSTPARSERParser.Unary_exprContext) parserCtx;
        //分两种情况处理
        ArrayList<PLCSymbol> exprList = visitor.visit(ctx.primary_expr());

        PLCVariable exprSymbol = (PLCVariable) exprList.get(0);
        int exprTypeId = exprSymbol.getTypeId();
        PLCTypeDeclSymbol symbolType = PLCTotalSymbolTable.getTypeByTypeID(exprTypeId);

        String operStr = ctx.getChild(0).getText();
        try{
            String exprVar = exprSymbol.getAssignVar();

            if(operStr.equals("NOT")){
                if(exprTypeId != IDGenerator.BOOL){
                    throw new PLCSemanticException("type mismatch : " + ctx.getText());
                }
                exprSymbol.setAssignVar("( ! " + exprVar + ")");
            }
            else if(operStr.equals("+") || operStr.equals("-")){
                if(!symbolType.checkCanMathCalcWith(IDGenerator.INTID)){
                    throw new PLCSemanticException("type mismatch : " + ctx.getText());
                }
                StringBuilder newVar = new StringBuilder();
                newVar.append("(").append(operStr).append(exprVar).append(")");
                exprSymbol.setAssignVar(new String(newVar));
            }
        }
        catch (PLCSemanticException e){
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        return exprList;
    }
}