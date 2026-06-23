package staticCheckVisitor.strategy.type_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.basicTypeTable;
import static antlr4.PLCSTPARSERParser.RULE_struct_type_access;

@StrategyForVisit(ruleIndex = RULE_struct_type_access)
public class VisitStruct_type_access implements Strategy {
    /**
     * 返回plc type的复制
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Struct_type_accessContext ctx = (PLCSTPARSERParser.Struct_type_accessContext) parserCtx;
        //获取命名空间作用域
        ArrayList<String> nameList = new ArrayList<>();
        for (PLCSTPARSERParser.Namespace_nameContext nameContext : ctx.namespace_name()) {
            nameList.add(nameContext.getText());
        }

        PLCScope npScope;
        if(nameList.isEmpty()){//当前作用域
            npScope = PLCScopeStack.currentScope;
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

        //获取变量名称
        String typeName = ctx.struct_type_name().getText();
        PLCStructDeclSymbol basicType = (PLCStructDeclSymbol) npScope.deepFindSymbol(typeName, PLCModifierEnum.Sort.STRUCT_DECL);
        if(basicType == null){
            throw new PLCSemanticException("can not find type : " + typeName);
        }

        //检查无误，进行组装
        PLCStructDeclSymbol targetVar = new PLCStructDeclSymbol(basicType);

        //设置runtimeName
        StringBuilder runtimeName = new StringBuilder();
        for (String name : nameList) {
            runtimeName.append(name).append(".");
        }
        runtimeName.append(basicType.getRuntimeName());
        targetVar.setRuntimeTypeName(new String(runtimeName));

        //打包返回
        return visitor.packSymbols(targetVar);

    }
}