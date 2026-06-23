package staticCheckVisitor.strategy.var_decls.other_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_loc_partly_var_decl;

@StrategyForVisit(ruleIndex = RULE_loc_partly_var_decl)
public class VisitLoc_partly_var_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Loc_partly_var_declContext ctx = (PLCSTPARSERParser.Loc_partly_var_declContext) parserCtx;
        PLCVariable varInfo = new PLCVariable();
        //'VAR'
        varInfo.setVarSections(PLCModifierEnum.VarSections.VAR);
        //RETAINORNONRETAIN?
        if(ctx.RETAINORNONRETAIN() != null){
            varInfo.setRetainQualifiers(ctx.RETAINORNONRETAIN().getText());
        }

        //vars
        ArrayList<PLCSymbol> vars = new ArrayList<>();
        for (PLCSTPARSERParser.Loc_partly_varContext varContext : ctx.loc_partly_var()) {
            vars.addAll(visitor.visit(varContext));
        }

        //在此层组装信息 在decl层加入符号表
        for (PLCSymbol var : vars) {
            visitor.visitorTool.settleVarAttrs(varInfo, (PLCVariable) var);
        }

        return vars;
    }
}
