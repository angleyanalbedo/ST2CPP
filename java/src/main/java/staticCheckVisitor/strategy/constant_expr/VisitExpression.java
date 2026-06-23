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
        else{  //有多个节点时，严格要求xor expr为bool类型
            StringBuilder exprVar = new StringBuilder();
            boolean ifConst = true;
            for(PLCSTPARSERParser.Xor_exprContext exprContext : ctx.xor_expr()){
                //进行类型检查
                PLCVariable xorTypeSymbol = (PLCVariable) visitor.visit(exprContext).get(0);
                if(!xorTypeSymbol.getIfConst()){
                    ifConst = false;
                }
                try{
                    if(xorTypeSymbol.getTypeId() != IDGenerator.BOOL){
                        throw new PLCSemanticException("type mismatch : " + ctx.getText());
                    }
                }
                catch (PLCSemanticException e){
                    System.err.println(e.getMessage());
                    throw new RuntimeException(e);
                }
                //检查无误，组装值信息
                exprVar.append("(").append(xorTypeSymbol.getAssignVar()).append(") || ");
            }
            //删除最后多余的" || "
            exprVar.delete(exprVar.length()-4, exprVar.length());
            //检查未发现错误，新建一个表示此层类型的符号，返回给上层
            PLCVariable boolSymbol = new PLCVariable();
            boolSymbol.setTypeId(IDGenerator.BOOL);
            boolSymbol.setSort(PLCModifierEnum.Sort.BOOL);
            boolSymbol.setAssignVar(new String(exprVar));
            boolSymbol.setIfConst(ifConst);
            return visitor.packSymbols(boolSymbol);
        }
    }

}
