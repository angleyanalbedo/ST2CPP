package staticCheckVisitor.strategy.var_decls.var_spec_init;

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

import static antlr4.PLCSTPARSERParser.RULE_struct_spec;

@StrategyForVisit(ruleIndex = RULE_struct_spec)
public class VisitStruct_spec implements Strategy{
    /**
     * 返回PLCStructDeclSymbol
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Struct_specContext ctx = (PLCSTPARSERParser.Struct_specContext) parserCtx;

        if (ctx.struct_decl()!=null) {  //struct_decl,声明新的结构体
            return visitor.visit(ctx.struct_decl());
        }else {   //struct_spec_init, 声明subtype
            PLCStructDeclSymbol structDeclSymbol;
            PLCVariable initVar = (PLCVariable) visitor.visit(ctx.struct_spec_init()).get(0);
            structDeclSymbol = (PLCStructDeclSymbol) initVar.getDeclSymbol();
            structDeclSymbol.setSort(PLCModifierEnum.Sort.STRUCT);
            return visitor.packSymbols(structDeclSymbol);
        }
    }
}
