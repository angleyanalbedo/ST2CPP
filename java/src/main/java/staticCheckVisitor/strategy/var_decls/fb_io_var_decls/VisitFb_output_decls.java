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

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_fb_output_decls;
import static antlr4.PLCSTPARSERParser.RULE_fb_io_var_decls;

@StrategyForVisit(ruleIndex = RULE_fb_output_decls)
public class VisitFb_output_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {

        PLCSTPARSERParser.Fb_output_declsContext ctx = (PLCSTPARSERParser.Fb_output_declsContext) parserCtx;

        PLCVariable varInfo = new PLCVariable();
        //section
        varInfo.setVarSections(PLCModifierEnum.VarSections.VAR_OUTPUT);

        // ( 'RETAIN' | 'NON_RETAIN' )?
        String modifier = ctx.getChild(1).getText();
        if(modifier.equals("RETAIN") || modifier.equals("NON_RETAIN")){
            varInfo.setRetainQualifiers(modifier);
        }

        //fb_output_decl
        ArrayList<PLCSymbol> vars = new ArrayList<>();
        for (PLCSTPARSERParser.Fb_output_declContext fb_output_declContext : ctx.fb_output_decl()) {
            vars.addAll(visitor.visit(fb_output_declContext));
        }

        //在此层组装信息 在decl层加入符号表
        for (PLCSymbol var : vars) {
            visitor.visitorTool.settleVarAttrs(varInfo, (PLCVariable) var);
        }
        return null;

    }
}