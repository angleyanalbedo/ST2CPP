package staticCheckVisitor.strategy.var_decls.global_var_decls;

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
import static antlr4.PLCSTPARSERParser.*;

@StrategyForVisit(ruleIndex = RULE_global_var_decls)
public class VisitGlobal_var_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Global_var_declsContext ctx = (PLCSTPARSERParser.Global_var_declsContext) parserCtx;
        PLCVariable infoVar = new PLCVariable();
        //section
        infoVar.setVarSections(PLCModifierEnum.VarSections.VAR_GLOBAL);

        // ( 'CONSTANT' | 'RETAIN' )?
        String modifier = ctx.getChild(1).getText();
        if(modifier.equals("CONSTANT")){
            infoVar.setIfConst(true);
        }else if(modifier.equals("RETAIN")){
            infoVar.setRetainQualifiers(PLCModifierEnum.RetainModifier.RETAIN);
        }

        //global_var_decl *
        ArrayList<PLCSymbol> vars = new ArrayList<>();
        for (Global_var_declContext globalVarDeclContext : ctx.global_var_decl()) {
            vars.addAll(visitor.visit(globalVarDeclContext));
        }

        //在此层组装信息 在decl层加入符号表
        for (PLCSymbol var : vars) {
            visitor.visitorTool.settleVarAttrs(infoVar, (PLCVariable) var);
        }

        return null;
    }
}