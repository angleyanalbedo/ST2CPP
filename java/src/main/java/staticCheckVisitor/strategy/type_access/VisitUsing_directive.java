package staticCheckVisitor.strategy.type_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCSymbols.PLCNamespaceDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_using_directive;

@StrategyForVisit(ruleIndex = RULE_using_directive)
public class VisitUsing_directive implements Strategy {
    /**
     * @describe 返回经过检查的 有RumtimeTypeName PLCTypeDeclSymbol(copy)
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Using_directiveContext ctx = (PLCSTPARSERParser.Using_directiveContext) parserCtx;
        ArrayList<PLCSymbol> usingNamespaceList = new ArrayList<>();
        //遍历每一个namespace_h_name，拿到符号名称序列，并进行检查
        for (PLCSTPARSERParser.Namespace_h_nameContext context : ctx.namespace_h_name()) {
            ArrayList<PLCSymbol> symbols = visitor.visit(context);
            PLCScope namespaceScope = visitor.visitorTool.findNestedNameSpaceScope(symbols);
            try{
                if(namespaceScope == null){
                    throw new PLCSemanticException("can not find namespace : " + context.getText());
                }
                PLCNamespaceDeclSymbol namespaceSymbol = (PLCNamespaceDeclSymbol) namespaceScope.getDeclSymbol();
                //使用构造函数拷贝，如果类型信息不足则再添加
                PLCNamespaceDeclSymbol newNamespaceSymbol = new PLCNamespaceDeclSymbol(namespaceSymbol);
                newNamespaceSymbol.setRuntimeTypeName(context.getText());
                usingNamespaceList.add(newNamespaceSymbol);
            }catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }

        }
        return usingNamespaceList;
    }
}