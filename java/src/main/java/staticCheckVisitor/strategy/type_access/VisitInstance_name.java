package staticCheckVisitor.strategy.type_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_instance_name;
@StrategyForVisit(ruleIndex = RULE_instance_name)
public class VisitInstance_name implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Instance_nameContext ctx = (PLCSTPARSERParser.Instance_nameContext) parserCtx;
        //获取命名空间作用域
        ArrayList<String> nameList = new ArrayList<>();
        for (PLCSTPARSERParser.Namespace_nameContext nameContext : ctx.namespace_name()) {
            nameList.add(nameContext.getText());
        }

        PLCScope npScope;
        if (nameList.isEmpty()) {//当前作用域
            npScope = PLCScopeStack.currentScope;
        } else {
            npScope = visitor.visitorTool.findNameSpaceScopeByNames(nameList);
            try {
                if (npScope == null) {
                    throw new PLCSemanticException("can not find scope: " + ctx.getText());
                }
            } catch (PLCSemanticException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }

        //获取变量名称
        String typeName = ctx.class_name().getText();
        PLCTypeDeclSymbol basicType = (PLCTypeDeclSymbol) npScope.deepFindSymbol(typeName);
        if (basicType == null) {
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
