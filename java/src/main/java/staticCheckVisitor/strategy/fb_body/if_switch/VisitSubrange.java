package staticCheckVisitor.strategy.fb_body.if_switch;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSubrangeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_subrange;

@StrategyForVisit(ruleIndex = RULE_subrange)
public class VisitSubrange implements Strategy {
    /**
     * 返回PLCSubrangeSymbol 已定义了上下限
     * 其他地方需要修改
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.SubrangeContext ctx = (PLCSTPARSERParser.SubrangeContext) parserCtx;
        PLCVariable constSymbol = (PLCVariable) visitor.visit(ctx.constant_expr(0)).get(0);
        PLCModifierEnum.Sort constSort = constSymbol.getSort();
        PLCVariable constSymbol_1 = (PLCVariable) visitor.visit(ctx.constant_expr(1)).get(0);
        PLCModifierEnum.Sort constSort_1 = constSymbol_1.getSort();
        try{
            if(constSort != PLCModifierEnum.Sort.INT || constSort_1 != PLCModifierEnum.Sort.INT){
                throw new PLCSemanticException("\ntype mismatch" + ctx.getText());
            }
            if(!constSymbol.getIfConst() || !constSymbol_1.getIfConst()){
                throw new PLCSemanticException("\nneed const : " + ctx.getText());
            }
        }catch(PLCSemanticException e){
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
        PLCSubrangeDeclSymbol targetSymbol = new PLCSubrangeDeclSymbol();
        targetSymbol.setLowerLimit(constSymbol.getAssignVar());
        targetSymbol.setUpperLimit(constSymbol_1.getAssignVar());

        return visitor.packSymbols(targetSymbol);
    }
}