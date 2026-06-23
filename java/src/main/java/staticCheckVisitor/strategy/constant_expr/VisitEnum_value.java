package staticCheckVisitor.strategy.constant_expr;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbols.PLCEnumDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static antlr4.PLCSTPARSERParser.RULE_enum_value;


@StrategyForVisit(ruleIndex = RULE_enum_value)
public class VisitEnum_value implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Enum_valueContext ctx = (PLCSTPARSERParser.Enum_valueContext) parserCtx;
        if(ctx.enum_type_name() != null){ //枚举常量
            //获取枚举类型名称
            String enumTypeName = ctx.enum_type_name().getText();
            //搜索类型
            PLCEnumDeclSymbol enumTypeSymbol = (PLCEnumDeclSymbol) currentScope.deepFindSymbol(enumTypeName, PLCModifierEnum.Sort.ENUM_DECL);
            try{
                if(enumTypeSymbol == null){
                    throw new PLCSemanticException("can not find enum type: " + enumTypeName);
                }
            }
            catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                System.exit(-1);
            }
            //搜索枚举常量名称
            String enumConstName = ctx.identifier().getText();
            PLCSymbol enumConstSymbol = enumTypeSymbol.getImportSymbolTable().findSymbol(enumConstName);

            if(enumConstSymbol == null){
                throw new PLCSemanticException("in enum type : " + enumTypeName + " can not find const : " + enumConstName);
            }

            //检查没有出错，打包返回
            //获得枚举常量的id
            PLCVariable conversedVar = (PLCVariable) enumConstSymbol;
            int enumConstTypeId = enumTypeSymbol.getEnumConstTypeId();
            PLCModifierEnum.Sort enumConstSort = enumTypeSymbol.getEnumConstSort();

            PLCVariable enumSymbol = new PLCVariable();
            enumSymbol.setTypeId(enumConstTypeId);
            enumSymbol.setSort(enumConstSort);
            enumSymbol.setIfConst(true);
            enumSymbol.setAssignVar(conversedVar.getUniqueName());
            return visitor.packSymbols(enumSymbol);

        }
        else{ //普通变量
            String varName = ctx.identifier().getText();
            PLCSymbol varSymbol = currentScope.deepFindSymbol(varName);
            try{
                if(!(varSymbol instanceof PLCVariable)){
                    throw new PLCSemanticException("undefined variable: " + ctx.getText());
                }
            }catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                System.exit(-1);
            }

            PLCVariable conversedVar = (PLCVariable) varSymbol;
            PLCVariable variable = new PLCVariable();
            variable.setTypeId(varSymbol.getTypeId());
            variable.setSort(varSymbol.getSort());
            String assignVar = conversedVar.getLocalScope() == currentScope?("*"+conversedVar.getName()):conversedVar.getUniqueName();
            variable.setAssignVar(assignVar);
            return visitor.packSymbols(variable);
        }
    }
}