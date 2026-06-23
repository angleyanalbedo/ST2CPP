package staticCheckVisitor.strategy.type_access;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_scope_name;

@StrategyForVisit(ruleIndex = RULE_scope_name)
public class VisitScope_name implements Strategy {
    /**
     * 返回只有名称信息的PLCVariable
     * 引用信息传递
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Scope_nameContext ctx = (PLCSTPARSERParser.Scope_nameContext) parserCtx;
        PLCVariable var = new PLCVariable();
        var.setName(ctx.identifier().getText());
        return visitor.packSymbols(var);
    }
}
