package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.basicTypeTable;
import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static antlr4.PLCSTPARSERParser.RULE_str_spec;

@StrategyForVisit(ruleIndex = RULE_str_spec)
public class VisitStr_spec implements Strategy {
    /**
     * 返回包含默认初始值、typeid和sort的PLCTypeDeclSymbol类
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Str_specContext ctx = (PLCSTPARSERParser.Str_specContext) parserCtx;
        //访问 ('STRING' | 'WSTRING')获得类型
        String typeName = ctx.getChild(0).getText();
        PLCTypeDeclSymbol typeSymbol = (PLCTypeDeclSymbol) basicTypeTable.findSymbol(typeName);
        PLCTypeDeclSymbol infoSymbol = new PLCTypeDeclSymbol();
        infoSymbol.setTypeId(typeSymbol.getTypeId());
        infoSymbol.setSort(typeSymbol.getVarSort());
        infoSymbol.setInitVar(typeSymbol.getInitVar());

        //访问identifier进行静态检查
        if(ctx.identifier() != null){
            String varName = ctx.identifier().getText();
            PLCSymbol targetSymbol = currentScope.deepFindSymbol(varName, PLCModifierEnum.Sort.INT);
            try{
                if(targetSymbol == null){
                    throw new PLCSemanticException(varName + "can not be length of STRING");
                }
            }
            catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                System.exit(-1);
            }

        }

        infoSymbol.setRuntimeTypeName(typeSymbol.getRuntimeName());

        //打包返回
        return visitor.packSymbols(infoSymbol);
    }
}
