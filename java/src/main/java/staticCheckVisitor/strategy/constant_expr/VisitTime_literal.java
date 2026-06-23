package staticCheckVisitor.strategy.constant_expr;

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

import static antlr4.PLCSTPARSERParser.RULE_time_literal;

@StrategyForVisit(ruleIndex = RULE_time_literal)
public class VisitTime_literal implements Strategy {
    /**
     * 时间类型待RunTime支持
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCVariable plcSymbol = new PLCVariable();
        plcSymbol.setTypeId(IDGenerator.TIME);
        plcSymbol.setSort(PLCModifierEnum.Sort.TIME);
        plcSymbol.setAssignVar("(*(new TIME(" + parserCtx.getText() + ")))");
        return visitor.packSymbols(plcSymbol);
    }
}