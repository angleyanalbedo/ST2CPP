package staticCheckVisitor.strategy.type_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCSymbols.PLCClassDeclSymbol;
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
import static PLCSymbolAndScope.PLCScopeStack.globalScope;
import static antlr4.PLCSTPARSERParser.RULE_interface_type_access;

@StrategyForVisit(ruleIndex = RULE_interface_type_access)
public class VisitInterface_type_access implements Strategy {
    /**
     * @describe 检查后打包返回
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Interface_type_accessContext ctx = (PLCSTPARSERParser.Interface_type_accessContext) parserCtx;

        //获取命名空间作用域
        ArrayList<String> nameList = new ArrayList<>();
        for (PLCSTPARSERParser.Namespace_nameContext nameContext : ctx.namespace_name()) {
            nameList.add(nameContext.getText());
        }

        PLCScope nameSpaceScope;
        if(nameList.isEmpty()){//当前作用域
            nameSpaceScope = currentScope.getParentScope();
        }else{
            nameSpaceScope = visitor.visitorTool.findNameSpaceScopeByNames(nameList);
            try{
                if(nameSpaceScope == null){
                    throw new PLCSemanticException("can not find scope: " + ctx.getText());
                }
            }catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }
        }

        //查找命名空间
        String typeName = ctx.interface_type_name().getText();
        PLCSymbol interfaceSymbol = nameSpaceScope.deepFindSymbol(typeName, PLCModifierEnum.Sort.INTERFACE_DECL);

        if(interfaceSymbol == null){
            throw new PLCSemanticException("can not find interface :" + typeName);
        }

        PLCInterfaceDeclSymbol targetVar = new PLCInterfaceDeclSymbol((PLCInterfaceDeclSymbol) interfaceSymbol);

        StringBuilder runtimeTypeName = new StringBuilder();
        for (PLCSTPARSERParser.Namespace_nameContext nameContext : ctx.namespace_name()) {
            runtimeTypeName.append(nameContext.getText()).append(".");
        }
        runtimeTypeName.append(interfaceSymbol.getRuntimeName());
        targetVar.setRuntimeTypeName(new String(runtimeTypeName));

        //打包返回
        return visitor.packSymbols(targetVar);
    }
}