package staticCheckVisitor.strategy.type_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbols.PLCEnumDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCInterfaceDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static antlr4.PLCSTPARSERParser.RULE_enum_type_access;

@StrategyForVisit(ruleIndex = RULE_enum_type_access)
public class VisitEnum_type_access implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Enum_type_accessContext ctx = (PLCSTPARSERParser.Enum_type_accessContext) parserCtx;

        //获取命名空间作用域
        ArrayList<String> nameList = new ArrayList<>();
        for (PLCSTPARSERParser.Namespace_nameContext nameContext : ctx.namespace_name()) {
            nameList.add(nameContext.getText());
        }

        PLCScope npScope;
        if(nameList.isEmpty()){//当前作用域
            npScope = PLCScopeStack.currentScope.getParentScope();
        }else{
            npScope = visitor.visitorTool.findNameSpaceScopeByNames(nameList);
            try{
                if(npScope == null){
                    throw new PLCSemanticException("can not find scope: " + ctx.getText());
                }
            }catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }


        //查找命名空间
        String typeName = ctx.enum_type_name().getText();
        PLCSymbol symbol = npScope.deepFindSymbol(typeName, PLCModifierEnum.Sort.ENUM_DECL);
        if(symbol == null){
            throw new PLCSemanticException("can not find enum type " + ctx.getText());
        }

        PLCEnumDeclSymbol targetVar = new PLCEnumDeclSymbol((PLCEnumDeclSymbol) symbol);

        StringBuilder runtimeTypeName = new StringBuilder();
        for (PLCSTPARSERParser.Namespace_nameContext nameContext : ctx.namespace_name()) {
            runtimeTypeName.append(nameContext.getText()).append(".");
        }
        runtimeTypeName.append(symbol.getRuntimeName());

        targetVar.setRuntimeTypeName(new String(runtimeTypeName));

        //打包返回
        return visitor.packSymbols(targetVar);
    }
}
