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

import static antlr4.PLCSTPARSERParser.RULE_add_expr;

@StrategyForVisit(ruleIndex = RULE_add_expr)
public class VisitAdd_expr implements Strategy {
    /**
     * 访问下层每一个节点，获取类型，比较是否可以计算(checkListIfCompatible)
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Add_exprContext ctx = (PLCSTPARSERParser.Add_exprContext) parserCtx;
        if(ctx.term().size() == 1){
            return visitor.visit(ctx.term(0));
        }
        else{
            ArrayList<PLCSymbol> exprList = new ArrayList<>();
            //访问下层节点，将符号存在exprList
            for(PLCSTPARSERParser.TermContext termContext : ctx.term()){
                exprList.add(visitor.visit(termContext).get(0));
            }

            //检查符号是否兼容
            PLCTypeDeclSymbol symbolType = PLCTotalSymbolTable.getTypeByTypeID(exprList.get(0).getTypeId());

            StringBuilder addVar = new StringBuilder();
            int operIndex = 1;

            for(PLCSymbol symbol : exprList){
                PLCVariable termSymbol = (PLCVariable) symbol;
                int typeId = symbol.getTypeId();
                try{
                    if(!symbolType.checkCanMathCalcWith(typeId)){
                        throw new PLCSemanticException("type mismatch : " + ctx.getText());
                    }
                }catch (PLCSemanticException e){
                    System.err.println(e.getMessage());
                    throw new RuntimeException(e);
                }

                //拼接字符串
                addVar.append("(").append(termSymbol.getAssignVar()).append(") ");

                if(operIndex < ctx.getChildCount()){
                    addVar.append(ctx.getChild(operIndex).getText());
                    operIndex += 2;
                }
            }

            PLCVariable addSymbol = (PLCVariable) exprList.get(0);
            addSymbol.setAssignVar(new String(addVar));
            return exprList;
        }
    }
}