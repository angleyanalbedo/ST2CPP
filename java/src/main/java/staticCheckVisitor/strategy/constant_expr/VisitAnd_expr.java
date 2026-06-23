package staticCheckVisitor.strategy.constant_expr;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_and_expr;


@StrategyForVisit(ruleIndex = RULE_and_expr)
public class VisitAnd_expr implements Strategy {
    /**
     * 处理同 visitExpression
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.And_exprContext ctx = (PLCSTPARSERParser.And_exprContext) parserCtx;
        if(ctx.compare_expr().size() == 1){ //只有一个节点时，不理会and expr的类型，直接访问返回
            return visitor.visit(ctx.getChild(0));
        }
        else{  //有多个节点时，严格要求and expr为bool类型
            StringBuilder andVar = new StringBuilder();
            boolean ifConst = true;
            for(PLCSTPARSERParser.Compare_exprContext exprContext : ctx.compare_expr()){
                PLCVariable compareSymbol = (PLCVariable) visitor.visit(exprContext).get(0);
                if(!compareSymbol.getIfConst()){
                    ifConst = false;
                }
                try{
                    if(compareSymbol.getTypeId() != IDGenerator.BOOL){
                        throw new PLCSemanticException("type mismatch : " + ctx.getText());
                    }
                }
                catch (PLCSemanticException e){
                    System.err.println(e.getMessage());
                    System.exit(-1);
                }

                andVar.append("(").append(compareSymbol.getAssignVar()).append(") & ");
            }
            andVar.delete(andVar.length()-2, andVar.length());
            //检查未发现错误，新建一个表示此层类型的符号，返回给上层
            PLCVariable boolSymbol = new PLCVariable();
            boolSymbol.setTypeId(IDGenerator.BOOL);
            boolSymbol.setSort(PLCModifierEnum.Sort.BOOL);
            boolSymbol.setAssignVar(new String(andVar));
            boolSymbol.setIfConst(ifConst);
            return visitor.packSymbols(boolSymbol);
        }
    }
}