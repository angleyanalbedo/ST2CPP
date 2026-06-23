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

@StrategyForVisit(ruleIndex = RULE_param_assign, branch = 2)
public class VisitOutParam implements Strategy {
    /**
     * 返回拥有typeid sort 名称和assign var var section的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.OutParamContext ctx = (PLCSTPARSERParser.OutParamContext) parserCtx;
        PLCVariable var = new PLCVariable();
        String varName = ctx.variable_name().getText();
        var.setName(varName);
        var.setRuntimeName("_" + varName + IDGenerator.getIDGenerator().newTempId());

        PLCVariable outputVar = (PLCVariable) visitor.visit(ctx.variable()).get(0);
        var.setSort(outputVar.getSort());
        var.setTypeId(outputVar.getTypeId());
        var.setAssignVar(outputVar.getRuntimeName());
        var.setVarSections(PLCModifierEnum.VarSections.VAR_OUTPUT);

        return visitor.packSymbols(var);
    }
}
