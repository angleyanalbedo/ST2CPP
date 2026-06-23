package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;
import java.util.Collection;

import static antlr4.PLCSTPARSERParser.RULE_struct_spec_init;

@StrategyForVisit(ruleIndex = RULE_struct_spec_init)
public class VisitStruct_spec_init implements Strategy {
    /**
     * 返回拥有类型信息和struct_init  PLCVairable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Struct_spec_initContext ctx = (PLCSTPARSERParser.Struct_spec_initContext) parserCtx;
        PLCVariable specVariable = new PLCVariable();
        //struct_type_access
        PLCTypeDeclSymbol structAccess = (PLCTypeDeclSymbol) visitor.visit(ctx.struct_type_access()).get(0);
        specVariable.setTypeId(structAccess.getTypeId());
        specVariable.setSort(structAccess.getVarSort());
        specVariable.setAssignVar(structAccess.getInitVar());
        specVariable.setRuntimeTypeName(structAccess.getRuntimeTypeName());

        PLCStructDeclSymbol symbolType = (PLCStructDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(structAccess.getTypeId());
        specVariable.setDeclSymbol(symbolType);

        //struct_init
        if(ctx.struct_init()!=null){
            PLCVariable initVar =(PLCVariable) visitor.visit(ctx.struct_init()).get(0);
            ArrayList<PLCVariable> structElements = symbolType.getVariables();
            PLCStructDeclSymbol initStruct = (PLCStructDeclSymbol) initVar.getDeclSymbol();
            ArrayList<PLCVariable> comparingElements = initStruct.getVariables();
            if(!new CompareStructElements().compareStructElements(structElements, comparingElements)){
                throw new PLCSemanticException("struct assign mismatched, from " + ctx.getText());
            }
        }

        //根据翻译需求修改assign var
        String assignVar = ctx.struct_init().getText();
        specVariable.setAssignVar(assignVar);

        return visitor.packSymbols(specVariable);
    }
}
