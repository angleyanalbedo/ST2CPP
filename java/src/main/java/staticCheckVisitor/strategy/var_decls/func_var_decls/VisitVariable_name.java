package staticCheckVisitor.strategy.var_decls.func_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_variable_list;
import static antlr4.PLCSTPARSERParser.RULE_variable_name;

@StrategyForVisit(ruleIndex = RULE_variable_name)
public class VisitVariable_name implements Strategy {
    /**
     * @describe 返回一个只拥有名称信息的变量
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Variable_nameContext ctx = (PLCSTPARSERParser.Variable_nameContext) parserCtx;
        PLCVariable targetVariable = new PLCVariable();
        targetVariable.setName(ctx.getText());
        return visitor.packSymbols(targetVariable);
    }
}
