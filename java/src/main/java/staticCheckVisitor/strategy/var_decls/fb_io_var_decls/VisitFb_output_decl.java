package staticCheckVisitor.strategy.var_decls.fb_io_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_fb_output_decl;

@StrategyForVisit(ruleIndex = RULE_fb_output_decl)
public class VisitFb_output_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Fb_output_declContext ctx = (PLCSTPARSERParser.Fb_output_declContext)parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}

