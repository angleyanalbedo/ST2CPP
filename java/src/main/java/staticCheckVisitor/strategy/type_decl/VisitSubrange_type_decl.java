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
import static antlr4.PLCSTPARSERParser.RULE_subrange_type_decl;

@StrategyForVisit(ruleIndex = RULE_subrange_type_decl)
public class VisitSubrange_type_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Subrange_type_declContext ctx = (PLCSTPARSERParser.Subrange_type_declContext) parserCtx;
        PLCTypeDeclSymbol plcTypeDeclSymbol;

        //检查是否重名
        String name = ctx.subrange_type_name().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, name);

        //subrange_spec_init
        PLCVariable subrangeVar = (PLCVariable) visitor.visit(ctx.subrange_spec_init()).get(0);
        PLCSubrangeDeclSymbol subrangeDeclSymbol = (PLCSubrangeDeclSymbol) (subrangeVar).getDeclSymbol();

        if (subrangeDeclSymbol.getSubTypeId()!=-1){
            //创建subrange Symbol
            plcTypeDeclSymbol = new PLCSubrangeDeclSymbol();
            PLCSubrangeDeclSymbol subRangeSymbol = (PLCSubrangeDeclSymbol)plcTypeDeclSymbol;
            subRangeSymbol.setName(name);
            subRangeSymbol.setUpperLimit(subrangeDeclSymbol.getUpperLimit());
            subRangeSymbol.setLowerLimit(subrangeDeclSymbol.getLowerLimit());
            subRangeSymbol.setInitVar(subrangeVar.getAssignVar());
            subRangeSymbol.setSubTypeId(subrangeDeclSymbol.getSubTypeId());

        }
        else {
            //创建subtype Symbol
            plcTypeDeclSymbol = new PLCSubtypeDeclSymbol();
            PLCSubtypeDeclSymbol subTypeSymbol = (PLCSubtypeDeclSymbol)plcTypeDeclSymbol;
            subTypeSymbol.setName(name);
            subTypeSymbol.setParentType(subrangeDeclSymbol);
            subTypeSymbol.setInitVar(subrangeVar.getAssignVar());
        }

        //分配符号id和类型id
        plcTypeDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        plcTypeDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

        //加入符号表
        currentSymbolTable.addSymbol(plcTypeDeclSymbol);

        //总表
        PLCTotalSymbolTable.addType(plcTypeDeclSymbol);
        PLCTotalSymbolTable.addSymbol(plcTypeDeclSymbol);

        return visitor.packSymbols(plcTypeDeclSymbol);
    }
}
