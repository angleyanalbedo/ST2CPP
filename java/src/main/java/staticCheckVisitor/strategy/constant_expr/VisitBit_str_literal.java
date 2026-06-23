package staticCheckVisitor.strategy.constant_expr;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_bit_str_literal;

@StrategyForVisit(ruleIndex = RULE_bit_str_literal)
public class VisitBit_str_literal implements Strategy {
    /**
     * 需要runtime支持
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCVariable plcSymbol = new PLCVariable();
        plcSymbol.setTypeId(IDGenerator.BITSTR);
        plcSymbol.setSort(PLCModifierEnum.Sort.BITSTR);

        StringBuilder assignVar = new StringBuilder();
        assignVar.append("(*").
                append("(new ").
                append("BITSTR").
                append("(").
                append(parserCtx.getText()).
                append(")").append(")").append(")");
        plcSymbol.setAssignVar(new String(assignVar));

        return visitor.packSymbols(plcSymbol);
    }
}