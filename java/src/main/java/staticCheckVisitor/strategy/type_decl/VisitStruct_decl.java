package staticCheckVisitor.strategy.type_decl;

import PLCException.PLCSemanticException;
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
import java.util.Objects;

import static antlr4.PLCSTPARSERParser.RULE_struct_decl;

@StrategyForVisit(ruleIndex = RULE_struct_decl)
public class VisitStruct_decl implements Strategy{
    /**
     * 返回一个元素齐全的PLCStructDeclSymbol
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Struct_declContext ctx = (PLCSTPARSERParser.Struct_declContext) parserCtx;
        PLCStructDeclSymbol structDeclSymbol = new PLCStructDeclSymbol();
        //访问struct_elem_decl，获得PLCVariable
        for (PLCSTPARSERParser.Struct_elem_declContext struct_elem_declContext : ctx.struct_elem_decl()) {
            PLCSymbol structElem = visitor.visit(struct_elem_declContext).get(0);
            structDeclSymbol.addVariable((PLCVariable) structElem);
        }

        //设置OVERLAP
        String checkOverlap = ctx.getChild(0).getText();
        if(checkOverlap.equals("OVERLAP")){
            structDeclSymbol.setIfOverlap(true);
        }
        return visitor.packSymbols(structDeclSymbol);
    }
}
