package staticCheckVisitor.strategy.pou_decl;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCProgramDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_prog_decl;

@StrategyForVisit(ruleIndex = RULE_prog_decl)
public class VisitProg_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor){
        PLCSTPARSERParser.Prog_declContext ctx = (PLCSTPARSERParser.Prog_declContext) parserCtx;

        //检查重名
        String name = ctx.prog_type_name().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, name);

        //创建Symbol
        PLCProgramDeclSymbol progSymbol = new PLCProgramDeclSymbol();
        progSymbol.setName(name);
        progSymbol.setRuntimeName(name);

        //分配符号id和类型id
        progSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        progSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

        //加入符号表
        currentSymbolTable.addSymbol(progSymbol);

        //入栈
        PLCScopeStack.push(progSymbol);

        //总表
        PLCTotalSymbolTable.addType(progSymbol);
        PLCTotalSymbolTable.addBlock(progSymbol);


        for(PLCSTPARSERParser.Io_var_declsContext declsContext : ctx.io_var_decls()){
            visitor.visit(declsContext);
        }

        for(PLCSTPARSERParser.Func_var_declsContext declsContext : ctx.func_var_decls()){
            visitor.visit(declsContext);
        }

        for(PLCSTPARSERParser.Temp_var_declsContext declsContext : ctx.temp_var_decls()){
            visitor.visit(declsContext);
        }


        for(PLCSTPARSERParser.Other_var_declsContext declsContext : ctx.other_var_decls()){
            visitor.visit(declsContext);
        }

        for(PLCSTPARSERParser.Loc_var_declsContext declContext : ctx.loc_var_decls()){
            visitor.visit(declContext);
        }

        for(PLCSTPARSERParser.Prog_access_declsContext declsContext : ctx.prog_access_decls()){
            visitor.visit(declsContext);
        }

        if(ctx.fb_body() != null){
            visitor.visit(ctx.fb_body());
        }

        //将prog作用域出栈
        PLCScopeStack.pop();
        return visitor.packSymbols(progSymbol);

    }
}
