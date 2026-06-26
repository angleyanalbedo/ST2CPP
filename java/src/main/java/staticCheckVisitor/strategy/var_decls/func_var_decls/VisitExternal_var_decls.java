package staticCheckVisitor.strategy.var_decls.func_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;
import java.util.Objects;

import static antlr4.PLCSTPARSERParser.RULE_external_var_decls;

@StrategyForVisit(ruleIndex = RULE_external_var_decls)
public class VisitExternal_var_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.External_var_declsContext ctx = (PLCSTPARSERParser.External_var_declsContext) parserCtx;
        PLCVariable tempVariable = new PLCVariable();
        tempVariable.ifConst = Objects.equals(ctx.getChild(1).getText(), "CONSTANT");
        tempVariable.varSections = PLCModifierEnum.VarSections.VAR_EXTERNAL;

        ArrayList<PLCSymbol> targetList = new ArrayList<>();

        for(PLCSTPARSERParser.External_declContext declContext: ctx.external_decl()){
            targetList.addAll(visitor.visit(declContext));
        }

        for (PLCSymbol var : targetList) {
            visitor.visitorTool.settleVarAttrs(tempVariable, (PLCVariable) var);
        }
        return targetList;
    }
}
