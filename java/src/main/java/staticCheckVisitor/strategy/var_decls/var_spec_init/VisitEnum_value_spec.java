package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_enum_value_spec;
import static PLCSymbolAndScope.PLCScopeStack.*;
@StrategyForVisit(ruleIndex = RULE_enum_value_spec)
public class VisitEnum_value_spec implements Strategy{
    /**
     * 返回一个拥有名称、初始值的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Enum_value_specContext ctx = (PLCSTPARSERParser.Enum_value_specContext) parserCtx;
        String name = ctx.identifier().getText();

        PLCVariable enumVar = new PLCVariable();
        enumVar.setName(name);
        enumVar.setIfConst(true);
        //int_literal
        if (ctx.int_literal()!=null) {
            PLCVariable plcSymbol = (PLCVariable) visitor.visit(ctx.int_literal()).get(0);
            enumVar.setSort(plcSymbol.getSort());
            enumVar.setAssignVar(plcSymbol.getAssignVar());
            enumVar.setTypeId(plcSymbol.getTypeId());
        }
        //constant_expr
        else if (ctx.constant_expr()!=null){
            PLCVariable plcSymbol = (PLCVariable) visitor.visit(ctx.constant_expr()).get(0);
            try{
                if (!plcSymbol.getIfConst()){
                    throw new PLCSemanticException(ctx.constant_expr().getText() + "is not static");
                }
            }catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }

            enumVar.setAssignVar(plcSymbol.getAssignVar());
            enumVar.setTypeId(plcSymbol.getTypeId());
            enumVar.setSort(plcSymbol.getSort());
        }

        return visitor.packSymbols(enumVar);
    }
}
