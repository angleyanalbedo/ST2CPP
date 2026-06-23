package staticCheckVisitor.strategy.type_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbols.PLCClassDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.*;
import static antlr4.PLCSTPARSERParser.RULE_class_type_access;
import static antlr4.PLCSTPARSERParser.RULE_simple_type_access;

@StrategyForVisit(ruleIndex = RULE_class_type_access)
public class VisitClass_type_access implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Class_type_accessContext ctx = (PLCSTPARSERParser.Class_type_accessContext) parserCtx;
        //获取命名空间作用域
        ArrayList<String> nameList = new ArrayList<>();
        for (PLCSTPARSERParser.Namespace_nameContext nameContext : ctx.namespace_name()) {
            nameList.add(nameContext.getText());
        }

        PLCScope nameSpaceScope;
        if(nameList.isEmpty()){//当前作用域
            PLCModifierEnum.Sort sort = currentScope.getDeclSymbol().getSort();
            if(sort == PLCModifierEnum.Sort.CLASS_DECL || sort == PLCModifierEnum.Sort.FB_DECL){
                //此时在class类型的作用域内，用于寻找基类, 需要向上一层
                nameSpaceScope = currentScope.getParentScope();
            }else{
                nameSpaceScope = currentScope;
            }
        }else{
            nameSpaceScope = visitor.visitorTool.findNameSpaceScopeByNames(nameList);
            try{
                if(nameSpaceScope == null){
                    throw new PLCSemanticException("can not find scope:  " + ctx.getText());
                }
            }catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }
        }

        //查找命名空间
        String typeName = ctx.class_type_name().getText();
        PLCSymbol classSymbol = nameSpaceScope.deepFindSymbol(typeName, PLCModifierEnum.Sort.CLASS_DECL);
        if(classSymbol == null){
            try {
                throw new PLCSemanticException("can not find class" + ctx.getText());
            } catch (PLCSemanticException e) {
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }
        }

        PLCClassDeclSymbol targetVar = new PLCClassDeclSymbol((PLCClassDeclSymbol) classSymbol);

        StringBuilder runtimeTypeName = new StringBuilder();
        for (PLCSTPARSERParser.Namespace_nameContext nameContext : ctx.namespace_name()) {
            runtimeTypeName.append(nameContext.getText()).append(".");
        }

        runtimeTypeName.append(classSymbol.getRuntimeName());

        targetVar.setRuntimeTypeName(new String(runtimeTypeName));
        //打包返回
        return visitor.packSymbols(targetVar);

    }
}