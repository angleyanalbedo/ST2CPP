package staticCheckVisitor.strategy.type_decl;

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

import static PLCSymbolAndScope.PLCScopeStack.*;
import static antlr4.PLCSTPARSERParser.RULE_struct_type_decl;

@StrategyForVisit(ruleIndex = RULE_struct_type_decl)
public class VisitStruct_type_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Struct_type_declContext ctx = (PLCSTPARSERParser.Struct_type_declContext) parserCtx;
        //检查是否重名
        String name = ctx.struct_type_name().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, name);

        //struct_spec
        PLCSymbol structSpec = visitor.visit(ctx.struct_spec()).get(0);
        if(structSpec.getSort() == PLCModifierEnum.Sort.STRUCT_DECL){     //新结构体声明
            PLCStructDeclSymbol structDeclSymbol = (PLCStructDeclSymbol) structSpec;
            structDeclSymbol.setName(name);
            //分配符号id和类型id
            structDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
            structDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

            //加入符号表
            currentSymbolTable.addSymbol(structDeclSymbol);

            //入栈
            PLCScopeStack.push(structDeclSymbol);

            //总表
            PLCTotalSymbolTable.addType(structDeclSymbol);
            PLCTotalSymbolTable.addBlock(structDeclSymbol);

            //出栈
            PLCScopeStack.pop();
            return visitor.packSymbols(structDeclSymbol);

        }else{ //struct_spec_init
            PLCVariable structVar = (PLCVariable) structSpec;
            PLCTypeDeclSymbol structDetail =  structVar.getDeclSymbol();
            PLCSubtypeDeclSymbol subtypeDecl = new PLCSubtypeDeclSymbol();
            subtypeDecl.setParentType(structDetail);

            //分配符号id和类型id
            subtypeDecl.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
            subtypeDecl.setTypeId(IDGenerator.getIDGenerator().newTypeId());

            //加入符号表
            currentSymbolTable.addSymbol(subtypeDecl);

            //总表
            PLCTotalSymbolTable.addType(subtypeDecl);
            PLCTotalSymbolTable.addSymbol(subtypeDecl);

            return visitor.packSymbols(subtypeDecl);
        }

    }
}
