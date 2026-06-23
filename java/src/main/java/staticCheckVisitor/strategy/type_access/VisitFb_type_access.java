package staticCheckVisitor.strategy.type_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbols.PLCClassDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCFBDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static antlr4.PLCSTPARSERParser.RULE_fb_type_access;

@StrategyForVisit(ruleIndex = RULE_fb_type_access)
public class VisitFb_type_access implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Fb_type_accessContext ctx = (PLCSTPARSERParser.Fb_type_accessContext) parserCtx;

        //获取命名空间作用域
        ArrayList<String> nameList = new ArrayList<>();
        for (PLCSTPARSERParser.Namespace_nameContext nameContext : ctx.namespace_name()) {
            nameList.add(nameContext.getText());
        }

        PLCScope npScope;
        if(nameList.isEmpty()){//当前作用域
            PLCModifierEnum.Sort sort = currentScope.getDeclSymbol().getSort();
            if(sort == PLCModifierEnum.Sort.FB_DECL){
                //此时在class类型的作用域内，用于寻找基类, 需要向上一层
                npScope = currentScope.getParentScope();
            }else{
                npScope = currentScope;
            }
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
        String typeName = ctx.fb_type_name().getText();
        PLCSymbol fbSymbol = npScope.deepFindSymbol(typeName, PLCModifierEnum.Sort.FB_DECL);
        if(fbSymbol == null){
            try {
                throw new PLCSemanticException("can not find fun body" + ctx.getText());
            } catch (PLCSemanticException e) {
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }
        }

        PLCFBDeclSymbol targetVar = new PLCFBDeclSymbol((PLCFBDeclSymbol) fbSymbol);

        StringBuilder runtimeTypeName = new StringBuilder();
        for (PLCSTPARSERParser.Namespace_nameContext nameContext : ctx.namespace_name()) {
            runtimeTypeName.append(nameContext.getText()).append(".");
        }

        runtimeTypeName.append(fbSymbol.getRuntimeName());

        targetVar.setRuntimeTypeName(new String(runtimeTypeName));

        //打包返回
        return visitor.packSymbols(targetVar);

    }
}
