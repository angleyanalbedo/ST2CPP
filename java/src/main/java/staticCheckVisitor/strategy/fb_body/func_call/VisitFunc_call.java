package staticCheckVisitor.strategy.fb_body.func_call;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_func_call;

@StrategyForVisit(ruleIndex = RULE_func_call)
public class VisitFunc_call implements Strategy {
    /**
     * 返回一个PLCVariable，
     * assignVar为完整的函数调用，
     * typeid为函数的返回值id（没有为-1）
     * sort为 函数返回值的sort（没有则为func_var）
     *
     *
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Func_callContext ctx = (PLCSTPARSERParser.Func_callContext) parserCtx;


        //获得函数符号
        PLCBaseFUNDeclSymbol fcCopy = (PLCBaseFUNDeclSymbol) visitor.visit(ctx.func_access()).get(0);
        PLCBaseFUNDeclSymbol fc = (PLCBaseFUNDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(fcCopy.getTypeId()
        );
        //获取参数列表
        ArrayList<PLCVariable> params = new ArrayList<>();
        for (PLCSTPARSERParser.Param_assignContext assignContext : ctx.param_assign()) {
            PLCVariable param = (PLCVariable) visitor.visit(assignContext).get(0);
            params.add(param);
        }

        //待返回的PLCVariable
        PLCVariable funcCallVar = new CheckFuncCall().checkFuncCall(fc, params, fcCopy.getRuntimeTypeName());
        return visitor.packSymbols(funcCallVar);
    }
}
