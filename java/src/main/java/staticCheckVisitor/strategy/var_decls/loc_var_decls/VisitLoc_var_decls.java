package staticCheckVisitor.strategy.var_decls.loc_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_loc_var_decls;

@StrategyForVisit(ruleIndex = RULE_loc_var_decls)
public class VisitLoc_var_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Loc_var_declsContext ctx = (PLCSTPARSERParser.Loc_var_declsContext) parserCtx;
        PLCVariable infoVar = new PLCVariable();
        //section
        infoVar.setVarSections(PLCModifierEnum.VarSections.VAR);

        // ( 'CONSTANT' | 'RETAIN' | 'NON_RETAIN' )?
        if(ctx.getChildCount() > 1) {
            String modifier = ctx.getChild(1).getText();
            if(!"VAR".equals(modifier)) {
                if("CONSTANT".equals(modifier)){
                    infoVar.setIfConst(true);
                }else if("RETAIN".equals(modifier)){
                    infoVar.setRetainQualifiers(PLCModifierEnum.RetainModifier.RETAIN);
                }
            }
        }

        //loc_var_decl *
        ArrayList<PLCSymbol> vars = new ArrayList<>();
        for (PLCSTPARSERParser.Loc_var_declContext loc_var_declContext : ctx.loc_var_decl()) {
            vars.addAll(visitor.visit(loc_var_declContext));
        }

        //在此层组装信息
        for (PLCSymbol var : vars) {
            visitor.visitorTool.settleVarAttrs(infoVar, (PLCVariable) var);
        }

        return vars;
    }
}