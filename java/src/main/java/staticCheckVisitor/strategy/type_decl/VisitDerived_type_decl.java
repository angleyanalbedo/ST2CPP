package staticCheckVisitor.strategy.type_decl;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCSubtypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_derived_type_decl;

@StrategyForVisit(ruleIndex = RULE_derived_type_decl)
public class VisitDerived_type_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Derived_type_declContext ctx = (PLCSTPARSERParser.Derived_type_declContext) parserCtx;
        //检查是否重名
        String name = ctx.derived_type_name().getText();
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

        //derived_spec_init
        PLCVariable plcSymbol = (PLCVariable) visitor.visit(ctx.derived_spec_init()).get(0);
        PLCTypeDeclSymbol parentType = PLCTotalSymbolTable.getTypeByTypeID(plcSymbol.getTypeId());
        plcTypeDeclSymbol.setInitVar(plcSymbol.getAssignVar());
        plcTypeDeclSymbol.setParentType(parentType);
        return visitor.packSymbols(plcTypeDeclSymbol);
    }
}
