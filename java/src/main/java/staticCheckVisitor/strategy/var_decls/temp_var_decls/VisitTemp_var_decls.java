package staticCheckVisitor.strategy.var_decls.temp_var_decls;

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
import static antlr4.PLCSTPARSERParser.RULE_temp_var_decls;

@StrategyForVisit(ruleIndex = RULE_temp_var_decls)
public class VisitTemp_var_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Temp_var_declsContext ctx = (PLCSTPARSERParser.Temp_var_declsContext) parserCtx;
        PLCVariable tempVarInfo = new PLCVariable();
        //section
        tempVarInfo.setVarSections(PLCModifierEnum.VarSections.VAR_TEMP);

        //temp vars
        ArrayList<PLCSymbol> vars = new ArrayList<>();
        for (PLCSTPARSERParser.Var_declContext var_declContext : ctx.var_decl()) {
            vars.addAll(visitor.visit(var_declContext));
        }

        for (PLCSTPARSERParser.Ref_var_declContext ref_var_declContext : ctx.ref_var_decl()) {
            vars.addAll(visitor.visit(ref_var_declContext));
        }

        //在此层组装信息 在decl层加入符号表
        for (PLCSymbol var : vars) {
            visitor.visitorTool.settleVarAttrs(tempVarInfo, (PLCVariable) var);
        }
        return null;
    }
}
