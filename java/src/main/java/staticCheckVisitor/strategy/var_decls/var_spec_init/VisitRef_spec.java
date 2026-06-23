package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;


import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_ref_spec;

@StrategyForVisit(ruleIndex = RULE_ref_spec)
public class VisitRef_spec implements Strategy {
    /**
     * 从总表内查找返回
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Ref_specContext ctx = (PLCSTPARSERParser.Ref_specContext) parserCtx;
        //获取数组名称
        PLCSymbol elementType = visitor.visit(ctx.data_type_access()).get(0);
        int elemTypeId = elementType.getTypeId();
        PLCRefDeclSymbol refType = PLCTotalSymbolTable.refSymbolMap.get(elemTypeId);

        PLCRefDeclSymbol infoSymbol = new PLCRefDeclSymbol(refType);

        infoSymbol.setRuntimeTypeName(refType.getRuntimeName() + "()");
        return visitor.packSymbols(infoSymbol);
    }
}
