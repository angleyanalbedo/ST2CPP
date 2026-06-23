package staticCheckVisitor.strategy.fb_body.func_call;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_param_assign;

@StrategyForVisit(ruleIndex = RULE_param_assign, branch = 0)
public class VisitInputParam implements Strategy {
    /**
     * 返回拥有typeid sort 名称和assign var var section的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.InputParamContext ctx = (PLCSTPARSERParser.InputParamContext) parserCtx;
        PLCVariable var = new PLCVariable();
        if(ctx.variable_name() != null){
            String variableName = ctx.variable_name().getText();
            var.setName(variableName);
            var.setRuntimeName("_" + variableName + IDGenerator.getIDGenerator().newTempId());
        }

        PLCVariable exprVar = (PLCVariable) visitor.visit(ctx.expression()).get(0);

        var.setTypeId(exprVar.getTypeId());
        var.setSort(exprVar.getSort());
        var.setAssignVar(exprVar.getAssignVar());
        var.setVarSections(PLCModifierEnum.VarSections.VAR_INPUT);

        return visitor.packSymbols(var);
    }
}
