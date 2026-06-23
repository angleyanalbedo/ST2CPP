package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCRefDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCRefVariable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_ref_spec_init;

@StrategyForVisit(ruleIndex = RULE_ref_spec_init)
public class VisitRef_spec_init implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Ref_spec_initContext ctx = (PLCSTPARSERParser.Ref_spec_initContext) parserCtx;
        //收集下层信息，获取类型信息
        PLCRefDeclSymbol refType = (PLCRefDeclSymbol) visitor.visit(ctx.ref_spec()).get(0);
        PLCRefDeclSymbol basicRefType = (PLCRefDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(refType.getTypeId());

        //要返回的符号
        PLCRefVariable refInitVar = new PLCRefVariable();
        refInitVar.setTypeId(basicRefType.getTypeId());
        refInitVar.setSort(basicRefType.getVarSort());
        refInitVar.setAssignVar(basicRefType.getInitVar());
        refInitVar.setRuntimeTypeName(basicRefType.getRuntimeTypeName());

        //访问下层
        if(ctx.ref_value() != null){
            PLCRefVariable refInit = (PLCRefVariable) visitor.visit(ctx.ref_value()).get(0);
            refInitVar.setAssignVar(refInit.getAssignVar());
            refInitVar.setReferredVariable(refInit.getReferredVariable());
        }
        return visitor.packSymbols(refInitVar);
    }
}
