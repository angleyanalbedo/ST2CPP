package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_user_defination_spec_init;
import static antlr4.PLCSTPARSERParser.RULE_var_decl_init;

@StrategyForVisit(ruleIndex = RULE_var_decl_init, branch = 1)
public class VisitDirectNum implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.DirectNumContext ctx = (PLCSTPARSERParser.DirectNumContext) parserCtx;
        PLCVariable variable = new PLCVariable();
        String location = ctx.located_at().Direct_variable().getText();
        String name;
        if(ctx.variable_name() != null){
            name = ctx.variable_name().getText();
        }else{
            name = "_varAt" + location;
        }
        variable.setName(name);
        variable.setLocation(location);

        PLCVariable varInfo = (PLCVariable) visitor.visit(ctx.loc_var_spec_init()).get(0);

        variable.setAssignVar(varInfo.getAssignVar());
        variable.setTypeId(varInfo.getTypeId());
        variable.setSort(varInfo.getSort());
        variable.setRuntimeTypeName(varInfo.getRuntimeTypeName());

        // 注册到符号表
        visitor.visitorTool.checkNameOnly(PLCScopeStack.currentSymbolTable, variable.getName());
        variable.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        PLCScopeStack.currentSymbolTable.addSymbol(variable);
        PLCTotalSymbolTable.addSymbol(variable);

        return visitor.packSymbols(variable);
    }
}
