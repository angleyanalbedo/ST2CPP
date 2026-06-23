package staticCheckVisitor.strategy.var_decls.io_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_input_decl;

@StrategyForVisit(ruleIndex = RULE_input_decl)
public class VisitInput_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Input_declContext ctx = (PLCSTPARSERParser.Input_declContext)parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}