package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCArrayDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_array_spec_init;

@StrategyForVisit(ruleIndex = RULE_array_spec_init)
public class VisitArray_spec_init implements Strategy {
    /**
     * 返回拥有初始值、typeid、sort RuntimeTypeName的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Array_spec_initContext ctx = (PLCSTPARSERParser.Array_spec_initContext) parserCtx;
        //收集下层信息，获取类型信息
        PLCTypeDeclSymbol arrayType = (PLCTypeDeclSymbol) visitor.visit(ctx.array_spec()).get(0);
        PLCArrayDeclSymbol basicArrayType = (PLCArrayDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(arrayType.getTypeId());

        //要返回的符号
        PLCVariable arrayInitVar = new PLCVariable();
        arrayInitVar.setTypeId(basicArrayType.getTypeId());
        arrayInitVar.setSort(basicArrayType.getVarSort());
        arrayInitVar.setAssignVar(basicArrayType.getInitVar());
        arrayInitVar.setRuntimeTypeName(basicArrayType.getRuntimeTypeName());

        //访问下层
        if(ctx.array_init() != null){
            PLCVariable arrayInit = (PLCVariable) visitor.visit(ctx.array_init()).get(0);
            arrayInitVar.setAssignVar(arrayInit.getAssignVar());
        }
        return visitor.packSymbols(arrayInitVar);
    }
}
