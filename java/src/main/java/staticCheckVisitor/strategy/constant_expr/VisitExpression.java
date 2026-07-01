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

import static antlr4.PLCSTPARSERParser.RULE_expression;

@StrategyForVisit(ruleIndex = RULE_expression)
public class VisitExpression implements Strategy {
    /**
     * 返回只具有类型id的符号
     * 分为两种情况处理：
     *  如果无比较动作，直接返回访问下层返回值
     *  如果有比较动作，检查类型 + 返回bool类型id符号
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.ExpressionContext ctx = (PLCSTPARSERParser.ExpressionContext) parserCtx;
        if(ctx.xor_expr().size() == 1){ //只有一个节点时，不理会xorexpr的类型，直接访问返回
            return visitor.visit(ctx.getChild(0));
        }
        else{  //有多个节点时
            StringBuilder exprVar = new StringBuilder();
            boolean ifConst = true;
            int resultTypeId = -1;
            PLCModifierEnum.Sort resultSort = null;
            for(PLCSTPARSERParser.Xor_exprContext exprContext : ctx.xor_expr()){
                PLCVariable xorTypeSymbol = (PLCVariable) visitor.visit(exprContext).get(0);
                if(!xorTypeSymbol.getIfConst()){
                    ifConst = false;
                }
                if(resultTypeId == -1){
                    resultTypeId = xorTypeSymbol.getTypeId();
                    resultSort = xorTypeSymbol.getSort();
                }
                //检查无误，组装值信息
                String op = (resultTypeId == IDGenerator.BOOL) ? "||" : "|";
                exprVar.append("(").append(xorTypeSymbol.getAssignVar()).append(") ").append(op).append(" ");
            }
            //删除最后多余的运算符
            String deleteOp = (resultTypeId == IDGenerator.BOOL) ? "|| " : "| ";
            exprVar.delete(exprVar.length() - deleteOp.length(), exprVar.length());
            //检查未发现错误，新建一个表示此层类型的符号，返回给上层
            PLCVariable boolSymbol = new PLCVariable();
            // 多态：BOOL 操作数返回 BOOL，ANY_BIT 返回操作数类型
            boolSymbol.setTypeId(resultTypeId);
            boolSymbol.setSort(resultSort);
            boolSymbol.setAssignVar(new String(exprVar));
            boolSymbol.setIfConst(ifConst);
            return visitor.packSymbols(boolSymbol);
        }
    }

}
