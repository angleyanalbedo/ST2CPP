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

import static antlr4.PLCSTPARSERParser.RULE_array_spec;
import static antlr4.PLCSTPARSERParser.RULE_array_spec_init;

@StrategyForVisit(ruleIndex = RULE_array_spec)
public class VisitArray_spec implements Strategy {
    /**
     * 如果此数组类型已经存在，则使用
     * 不存在则在global新建
     * 数组名称统一为_ARRAY_(TYPEID)
     * 返回包含默认初始值、typeid和sort的PLCTypeDeclSymbol类
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Array_specContext ctx = (PLCSTPARSERParser.Array_specContext) parserCtx;
        //获取数组名称
        PLCSymbol elementType = visitor.visit(ctx.data_type_access()).get(0);
        int elemTypeId = elementType.getTypeId();

        int dimension = ctx.subrange().size();

        if(dimension > GenerateArrayTypes.MAX_DIMENSION){
            throw new PLCSemanticException("max dimension is 15, from : " + ctx.getText());
        }

        for (PLCSTPARSERParser.SubrangeContext subrangeContext : ctx.subrange()) {
            visitor.visit(subrangeContext);
        }
        PLCArrayDeclSymbol arrayDeclSymbol = PLCTotalSymbolTable.arraySymbolMap.get(elemTypeId).get(dimension-1);

        PLCTypeDeclSymbol infoSymbol = new PLCTypeDeclSymbol();
        infoSymbol.setInitVar(arrayDeclSymbol.getInitVar());
        infoSymbol.setTypeId(arrayDeclSymbol.getTypeId());
        infoSymbol.setSort(arrayDeclSymbol.getSort());
        infoSymbol.setRuntimeTypeName(arrayDeclSymbol.getRuntimeName() + "()");
        return visitor.packSymbols(infoSymbol);
    }
}
