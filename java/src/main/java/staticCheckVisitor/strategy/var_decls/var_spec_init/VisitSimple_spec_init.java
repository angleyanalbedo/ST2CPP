package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_simple_spec_init;

@StrategyForVisit(ruleIndex = RULE_simple_spec_init)
    public class VisitSimple_spec_init implements Strategy {
    /**
     * 返回拥有初始值、typeid、sort runtimeTypeName的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Simple_spec_initContext ctx = (PLCSTPARSERParser.Simple_spec_initContext) parserCtx;

        //新建符号
        PLCVariable simpleTypeSymbol = new PLCVariable();

        //获取类型信息
        PLCTypeDeclSymbol typeSymbol = (PLCTypeDeclSymbol) visitor.visit(ctx.simple_spec()).get(0);

        //组装
        simpleTypeSymbol.setTypeId(typeSymbol.getTypeId());
        simpleTypeSymbol.setSort(typeSymbol.getVarSort());

        simpleTypeSymbol.setAssignVar(typeSymbol.getInitVar());
        simpleTypeSymbol.setRuntimeTypeName(typeSymbol.getRuntimeTypeName());

        PLCTypeDeclSymbol symbolType = PLCTotalSymbolTable.getTypeByTypeID(typeSymbol.getTypeId());

        if(ctx.constant_expr() != null){
            //获取表达式信息
            PLCVariable exprSymbol = (PLCVariable) visitor.visit(ctx.constant_expr()).get(0);
            if(!symbolType.checkCanAssignWith(exprSymbol.getTypeId())){
                throw new PLCSemanticException("type mismatch : " + ctx.getText());
            }

            //检查无误，将初始化信息填入符号
            simpleTypeSymbol.setAssignVar(exprSymbol.getAssignVar());

        }
        //返回包含类型id和初始值信息的一个符号
        return visitor.packSymbols(simpleTypeSymbol);
    }
}
