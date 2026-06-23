package staticCheckVisitor.strategy.var_decls.var_spec_init;

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
import static PLCSymbolAndScope.PLCScopeStack.globalScope;
import static antlr4.PLCSTPARSERParser.RULE_enum_spec_init;

@StrategyForVisit(ruleIndex = RULE_enum_spec_init)
public class VisitEnum_spec_init implements Strategy{
    /**
     * 返回PLCVariable
     * 其中含有属性：
     * declTypeSymbol(可能是已存在的或者是未存在的，未存在的typeid设置为-1)
     * assignVar
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx,PLCVisitor visitor){
        PLCSTPARSERParser.Enum_spec_initContext ctx = (PLCSTPARSERParser.Enum_spec_initContext) parserCtx;

        //新建符号
        PLCEnumDeclSymbol enumTypeSymbol;
        if(ctx.enum_type_access() == null){  //创建新类型
            enumTypeSymbol = new PLCEnumDeclSymbol();
            for (PLCSTPARSERParser.IdentifierContext context : ctx.identifier()) {
                String enumVarName = context.getText();
                PLCVariable enumVar = new PLCVariable();
                enumVar.setName(enumVarName);
                enumVar.setIfConst(true);
                enumTypeSymbol.addEnumValue(enumVar);
            }
        }else{  //复用旧类型
            //枚举类的复制
            enumTypeSymbol = (PLCEnumDeclSymbol) visitor.visit(ctx.enum_type_access()).get(0);
        }

        PLCVariable enumVar = new PLCVariable();
        enumVar.setDeclSymbol(enumTypeSymbol);

        if (ctx.enum_value()!=null){
            String enumName = ctx.enum_value().identifier().getText();
            PLCVariable value = enumTypeSymbol.findEnumValue(enumName);
            enumTypeSymbol.setInitEnumVar(value);
            enumVar.setAssignVar(value.getRuntimeName());
        }

        return visitor.packSymbols(enumVar);
    }
}
