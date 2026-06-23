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

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_no_retain_var_decls;

@StrategyForVisit(ruleIndex = RULE_no_retain_var_decls)
public class VisitNo_retain_var_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.No_retain_var_declsContext ctx = (PLCSTPARSERParser.No_retain_var_declsContext) parserCtx;
        PLCVariable varInfo = new PLCVariable();
        //section
        varInfo.setVarSections(PLCModifierEnum.VarSections.VAR);
        //retain
        varInfo.setRetainQualifiers(PLCModifierEnum.RetainModifier.NON_RETAIN);
        //access type
        if(ctx.Access_Spec() != null){
            String access = ctx.Access_Spec().getText();
            varInfo.setAccessModifier(visitor.visitorTool.getAccessType(access));
        }

        //vars
        ArrayList<PLCSymbol> vars = new ArrayList<>();
        //获得变量
        for (PLCSTPARSERParser.Var_decl_initContext var_decl_initContext : ctx.var_decl_init()) {
            vars.addAll(visitor.visit(var_decl_initContext));
        }

        //在此层组装信息 在decl层加入符号表
        for (PLCSymbol var : vars) {
            visitor.visitorTool.settleVarAttrs(varInfo, (PLCVariable) var);
        }

        return vars;
    }
}