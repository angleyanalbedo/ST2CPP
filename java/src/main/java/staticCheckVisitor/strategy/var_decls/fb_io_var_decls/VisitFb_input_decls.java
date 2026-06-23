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
import static antlr4.PLCSTPARSERParser.RULE_fb_input_decls;
import static antlr4.PLCSTPARSERParser.RULE_fb_io_var_decls;

@StrategyForVisit(ruleIndex = RULE_fb_input_decls)
public class VisitFb_input_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Fb_input_declsContext ctx = (PLCSTPARSERParser.Fb_input_declsContext) parserCtx;

        PLCVariable varInfo = new PLCVariable();
        //section
        varInfo.setVarSections(PLCModifierEnum.VarSections.VAR_INPUT);

        // ( 'RETAIN' | 'NON_RETAIN' )?
        String modifier = ctx.getChild(1).getText();
        if(modifier.equals("RETAIN") || modifier.equals("NON_RETAIN")){
            varInfo.setRetainQualifiers(modifier);
        }

        //fb_input_decl
        ArrayList<PLCSymbol> vars = new ArrayList<>();
        for (PLCSTPARSERParser.Fb_input_declContext fb_input_declContext : ctx.fb_input_decl()) {
            vars.addAll(visitor.visit(fb_input_declContext));
        }

        //在此层组装信息 在decl层加入符号表
        for (PLCSymbol var : vars) {
            visitor.visitorTool.settleVarAttrs(varInfo, (PLCVariable) var);
        }
        return null;

    }
}