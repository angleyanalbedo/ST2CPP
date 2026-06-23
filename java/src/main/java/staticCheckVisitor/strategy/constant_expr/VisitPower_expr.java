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

import static antlr4.PLCSTPARSERParser.RULE_power_expr;

@StrategyForVisit(ruleIndex = RULE_power_expr)
public class VisitPower_expr implements Strategy {
    /**
     * 同 visitAdd_expr(兼容检查有区别)
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Power_exprContext ctx = (PLCSTPARSERParser.Power_exprContext) parserCtx;
        if(ctx.unary_expr().size() == 1){
            return visitor.visit(ctx.unary_expr(0));
        }
        else{
            ArrayList<PLCSymbol> exprList = new ArrayList<>();
            //访问下层节点，将符号存在exprList
            for(PLCSTPARSERParser.Unary_exprContext exprContext : ctx.unary_expr()){
                exprList.addAll(visitor.visit(exprContext));
            }
            //检查符号是否兼
            PLCTypeDeclSymbol symbolType = PLCTotalSymbolTable.getTypeByTypeID(exprList.get(0).getTypeId());
            StringBuilder frontVar = new StringBuilder();
            StringBuilder backwardVar = new StringBuilder();
            for(PLCSymbol symbol : exprList){
                int typeId = symbol.getTypeId();
                try{
                    if(!symbolType.checkCanMathCalcWith(typeId)){
                        throw new PLCSemanticException("type mismatch : " + ctx.getText());
                    }

                    PLCVariable unaryExpr = (PLCVariable) symbol;
                    if(exprList.indexOf(symbol) != exprList.size()-1){
                        frontVar.append("POW(").append(unaryExpr.getAssignVar()).append(", ");
                        backwardVar.append(")");
                    }else{
                        frontVar.append(unaryExpr.getAssignVar());
                        frontVar.append(backwardVar);
                    }

                }catch (PLCSemanticException e){
                    System.err.println(e.getMessage());
                    System.exit(-1);
                }
            }

            PLCVariable powExpr = (PLCVariable) exprList.get(0);
            powExpr.setAssignVar(new String(frontVar));
            return visitor.packSymbols(powExpr);
        }
    }
}