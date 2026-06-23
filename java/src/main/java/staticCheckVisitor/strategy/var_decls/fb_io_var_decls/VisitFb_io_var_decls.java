package staticCheckVisitor.strategy.var_decls.fb_io_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_fb_decl;
import static antlr4.PLCSTPARSERParser.RULE_fb_io_var_decls;

@StrategyForVisit(ruleIndex = RULE_fb_io_var_decls)
public class VisitFb_io_var_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Fb_io_var_declsContext ctx = (PLCSTPARSERParser.Fb_io_var_declsContext) parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}