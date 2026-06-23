package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCArrayDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.GenerateArrayTypes;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_array_conformand;

@StrategyForVisit(ruleIndex = RULE_array_conformand)
public class VisitArray_conformand implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Array_conformandContext ctx = (PLCSTPARSERParser.Array_conformandContext) parserCtx;
        //获取数组名称
        PLCSymbol elementType = visitor.visit(ctx.data_type_access()).get(0);
        int elemTypeId = elementType.getTypeId();
        int dim=ctx.getChildCount()/2-2;
        if(dim > GenerateArrayTypes.MAX_DIMENSION){
            throw new PLCSemanticException("max dimension is 15, from :" + ctx.getText());
        }

        PLCArrayDeclSymbol arrayDeclSymbol = PLCTotalSymbolTable.arraySymbolMap.get(elemTypeId).get(dim);
        return visitor.packSymbols(arrayDeclSymbol);
    }
}
