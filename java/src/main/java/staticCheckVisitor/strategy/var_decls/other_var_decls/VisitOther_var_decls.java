package staticCheckVisitor.strategy.var_decls.other_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_other_var_decls;
import static antlr4.PLCSTPARSERParser.RULE_temp_var_decls;

@StrategyForVisit(ruleIndex = RULE_other_var_decls)
public class VisitOther_var_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Other_var_declsContext ctx = (PLCSTPARSERParser.Other_var_declsContext) parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}
