package staticCheckVisitor.strategy.type_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_simple_type_access;

@StrategyForVisit(ruleIndex = RULE_simple_type_access)
public class VisitSimple_type_access implements Strategy {
    /**
     *
     * 同VisitUser_defination_type_access,type_access返回值typedecl
     * 存在 ( namespace_name '.' )* 时，runtimeName会带有命名空间信息
     *
     * */

    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Simple_type_accessContext ctx = (PLCSTPARSERParser.Simple_type_accessContext) parserCtx;
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
        String typeName = ctx.simple_type_name().getText();
        PLCTypeDeclSymbol basicType = (PLCTypeDeclSymbol) npScope.deepFindSymbol(typeName);
        if(basicType == null){
            throw new PLCSemanticException("\ncan not find type : " + typeName);
        }
        //检查无误，进行组装
        PLCTypeDeclSymbol targetVar = new PLCTypeDeclSymbol(basicType);
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