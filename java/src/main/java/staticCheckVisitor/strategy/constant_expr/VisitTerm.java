package staticCheckVisitor.strategy.constant_expr;

import PLCException.PLCSemanticException;
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

import static antlr4.PLCSTPARSERParser.RULE_term;

@StrategyForVisit(ruleIndex = RULE_term)
public class VisitTerm implements Strategy {
    /**
     * 同 visitAdd_expr
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.TermContext ctx = (PLCSTPARSERParser.TermContext) parserCtx;
        if(ctx.power_expr().size() == 1){
            return visitor.visit(ctx.power_expr(0));
        }
        else{
            ArrayList<PLCSymbol> exprList = new ArrayList<>();
            //访问下层节点，将符号存在exprList
            for(PLCSTPARSERParser.Power_exprContext exprContext : ctx.power_expr()){
                exprList.add(visitor.visit(exprContext).get(0));
            }
            //检查符号是否兼
            PLCTypeDeclSymbol symbolType = PLCTotalSymbolTable.getTypeByTypeID(exprList.get(0).getTypeId());
            StringBuilder termVar = new StringBuilder();
            int operIndex = 1;
            for(PLCSymbol symbol : exprList){
                PLCVariable powExpr = (PLCVariable) symbol;
                int typeId = symbol.getTypeId();
                try{
                    if(!symbolType.checkCanMathCalcWith(typeId)){
                        throw new PLCSemanticException("type mismatch : " + ctx.getText());
                    }
                }catch (PLCSemanticException e){
                    System.err.println(e.getMessage());
                    System.exit(-1);
                }

                //拼接字符串
                termVar.append("(").append(powExpr.getAssignVar()).append(") ");
                if(operIndex < ctx.getChildCount()){
                    termVar.append(ctx.getChild(operIndex).getText());
                    operIndex += 2;
                }
            }

            PLCVariable termSymbol = (PLCVariable) exprList.get(0);
            termSymbol.setAssignVar(new String(termVar));
            return exprList;
        }
    }
}