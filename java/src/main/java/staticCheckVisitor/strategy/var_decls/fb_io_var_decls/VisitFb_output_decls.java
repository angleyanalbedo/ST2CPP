package staticCheckVisitor.strategy.var_decls.fb_io_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_fb_output_decls;

@StrategyForVisit(ruleIndex = RULE_fb_output_decls)
public class VisitFb_output_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Fb_output_declsContext ctx = (PLCSTPARSERParser.Fb_output_declsContext) parserCtx;

        ArrayList<PLCSymbol> allVars = new ArrayList<>();
        for (PLCSTPARSERParser.Fb_output_declContext fbOutputDecl : ctx.fb_output_decl()) {
            ArrayList<PLCSymbol> vars = visitor.visit(fbOutputDecl);
            for (PLCSymbol sym : vars) {
                if (sym instanceof PLCVariable pv) {
                    pv.setVarSections(PLCModifierEnum.VarSections.VAR_OUTPUT);
                }
            }
            allVars.addAll(vars);
        }
        return allVars;
    }
}
