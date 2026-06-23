package staticCheckVisitor.strategy.type_decl;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.*;
import static antlr4.PLCSTPARSERParser.RULE_array_type_decl;

@StrategyForVisit(ruleIndex = RULE_array_type_decl)
public class VisitArray_type_decl implements Strategy {
    /**
     * 给数组类起别名
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Array_type_declContext ctx = (PLCSTPARSERParser.Array_type_declContext) parserCtx;
        //检查是否重名
        String name = ctx.array_type_name().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, name);

        //创建
        PLCSubtypeDeclSymbol plcTypeDeclSymbol = new PLCSubtypeDeclSymbol();
        plcTypeDeclSymbol.setName(name);

        //分配符号id和类型id
        plcTypeDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        plcTypeDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

        //加入符号表
        currentSymbolTable.addSymbol(plcTypeDeclSymbol);

        //总表
        PLCTotalSymbolTable.addType(plcTypeDeclSymbol);
        PLCTotalSymbolTable.addSymbol(plcTypeDeclSymbol);

        //array_spec_init
        PLCVariable plcSymbol = (PLCVariable) visitor.visit(ctx.array_spec_init()).get(0);
        PLCTypeDeclSymbol parentType = PLCTotalSymbolTable.getTypeByTypeID(plcSymbol.getTypeId());
        plcTypeDeclSymbol.setInitVar(plcSymbol.getAssignVar());
        plcTypeDeclSymbol.setParentType(parentType);
        return visitor.packSymbols(plcTypeDeclSymbol);
    }
}
