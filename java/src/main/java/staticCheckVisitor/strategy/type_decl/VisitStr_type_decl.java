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
import static antlr4.PLCSTPARSERParser.RULE_str_type_decl;

@StrategyForVisit(ruleIndex = RULE_str_type_decl)
public class VisitStr_type_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Str_type_declContext ctx = (PLCSTPARSERParser.Str_type_declContext) parserCtx;
        //检查是否重名
        String name = ctx.string_type_name_identifier().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, name);

        //创建stringSymbol
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

        //str_spec
        PLCTypeDeclSymbol plcSymbol = (PLCTypeDeclSymbol) visitor.visit(ctx.str_spec()).get(0);

        //char_str
        if(ctx.char_str()!=null){
            plcSymbol.setInitVar(ctx.char_str().getText());
        }
        //str_spec_init

            plcTypeDeclSymbol.setInitVar(plcSymbol.getInitVar());
            plcTypeDeclSymbol.setParentType(PLCTotalSymbolTable.getTypeByTypeID(plcSymbol.getTypeId()));

        return visitor.packSymbols(plcTypeDeclSymbol);
    }
}
