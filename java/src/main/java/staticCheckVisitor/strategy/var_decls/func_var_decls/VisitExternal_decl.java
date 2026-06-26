package staticCheckVisitor.strategy.var_decls.func_var_decls;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_external_decl;

@StrategyForVisit(ruleIndex = RULE_external_decl)
public class VisitExternal_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.External_declContext ctx = (PLCSTPARSERParser.External_declContext) parserCtx;

        PLCSymbol typeSymbol = visitor.visit(ctx.getChild(2)).get(0);

        PLCVariable returnVar = new PLCVariable();

        returnVar.setName(ctx.global_var_name().getText());

        if (typeSymbol instanceof PLCVariable varInfo) {
            returnVar.setTypeId(varInfo.getTypeId());
            returnVar.setSort(varInfo.getSort());
            returnVar.setAssignVar(varInfo.getAssignVar());
            returnVar.setDeclSymbol(varInfo.getDeclSymbol());
            returnVar.setRuntimeTypeName(varInfo.getRuntimeName());
        } else if (typeSymbol instanceof PLCTypeDeclSymbol typeDecl) {
            returnVar.setTypeId(typeDecl.getTypeId());
            returnVar.setSort(typeDecl.getSort());
            returnVar.setAssignVar(typeDecl.getInitVar());
            returnVar.setDeclSymbol(typeDecl);
            returnVar.setRuntimeTypeName(typeDecl.getRuntimeName());
        }

        visitor.visitorTool.checkNameOnly(PLCScopeStack.currentSymbolTable, returnVar.getName());

        returnVar.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        PLCScopeStack.currentSymbolTable.addSymbol(returnVar);
        PLCTotalSymbolTable.addSymbol(returnVar);

        return visitor.packSymbols(returnVar);
    }
}
