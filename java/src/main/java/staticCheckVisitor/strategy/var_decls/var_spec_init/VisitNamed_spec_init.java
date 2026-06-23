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
import java.util.HashSet;


import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_named_spec_init;

@StrategyForVisit(ruleIndex = RULE_named_spec_init)
public class VisitNamed_spec_init implements Strategy{
    /**
     * 返回拥有
     * decl symbol（收集了类型信息）
     * 初始值initVal 的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx,PLCVisitor visitor) {
        PLCSTPARSERParser.Named_spec_initContext ctx = (PLCSTPARSERParser.Named_spec_initContext) parserCtx;

        PLCVariable namedNamedSpec = new PLCVariable();
        PLCEnumDeclSymbol enumTypeSymbol = new PLCEnumDeclSymbol();
        namedNamedSpec.setDeclSymbol(enumTypeSymbol);
        //enum_value_spec
        HashSet<String> nameSet = new HashSet<>();
        for (PLCSTPARSERParser.Enum_value_specContext specContext : ctx.enum_value_spec()) {
            PLCVariable namedTypeVariable = (PLCVariable) visitor.visit(specContext).get(0);
            enumTypeSymbol.addEnumValue(namedTypeVariable);

            //重名检查
            String name = namedTypeVariable.getName();
            try{
                if(nameSet.contains(name)){
                    throw new PLCSemanticException("duplicate name :" + name);
                }
            }catch (PLCSemanticException e){
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }
            nameSet.add(name);
        }

        //enum_spec
        if (ctx.enum_value()!=null){
            String enumName = ctx.enum_value().identifier().getText();
            PLCVariable enumValue = enumTypeSymbol.findEnumValue(enumName);
            try{
                if(enumValue == null){
                    throw new PLCSemanticException("enum const var inexist :" + ctx.getText());
                }
            }catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }
            enumTypeSymbol.setInitVar(enumValue.getAssignVar());
            enumTypeSymbol.setInitEnumVar(enumValue);
            namedNamedSpec.setAssignVar(enumValue.getRuntimeName());
        }
        return visitor.packSymbols(namedNamedSpec);
    }
}
