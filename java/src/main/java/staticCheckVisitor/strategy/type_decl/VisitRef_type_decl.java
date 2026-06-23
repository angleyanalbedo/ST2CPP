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

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_ref_type_decl;

@StrategyForVisit(ruleIndex = RULE_ref_type_decl)
public class VisitRef_type_decl implements Strategy {
    /**
     * 给ref起别名
     *此处应为给已存在的ref起别名
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Ref_type_declContext ctx = (PLCSTPARSERParser.Ref_type_declContext) parserCtx;
        //检查是否重名
        String name = ctx.ref_type_name().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, name);

        //创建
        PLCRefDeclSymbol plcRefDeclSymbol = new PLCRefDeclSymbol();
        plcRefDeclSymbol.setName(name);

        //分配符号id和类型id
        plcRefDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        plcRefDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

        //加入符号表
        currentSymbolTable.addSymbol(plcRefDeclSymbol);

        //总表
        PLCTotalSymbolTable.addType(plcRefDeclSymbol);
        PLCTotalSymbolTable.addSymbol(plcRefDeclSymbol);

        //ref_spec_init
        PLCRefVariable plcSymbol = (PLCRefVariable) visitor.visit(ctx.ref_spec_init()).get(0);
        //PLCTypeDeclSymbol parentType = PLCTotalSymbolTable.getTypeByTypeID(plcSymbol.getTypeId());
        plcRefDeclSymbol.setInitVar(plcSymbol.getAssignVar());
        plcRefDeclSymbol.setReferredTypeId(plcSymbol.getTypeId());
        return visitor.packSymbols(plcRefDeclSymbol);
    }
}
