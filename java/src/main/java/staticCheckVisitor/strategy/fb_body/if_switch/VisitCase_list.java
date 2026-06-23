package staticCheckVisitor.strategy.fb_body.if_switch;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_case_list;

@StrategyForVisit(ruleIndex = RULE_case_list)
public class VisitCase_list implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Case_listContext ctx = (PLCSTPARSERParser.Case_listContext) parserCtx;

        ArrayList<PLCSymbol> caseList = new ArrayList<>();
        for (PLCSTPARSERParser.Case_list_elemContext caseListElemContext : ctx.case_list_elem()) {
            PLCSymbol elemSymbol = visitor.visit(caseListElemContext).get(0);
            PLCModifierEnum.Sort elemSort = elemSymbol.getSort();
            try{
                if(elemSort == PLCModifierEnum.Sort.SUBRANGE_DECL){
                    caseList.add(elemSymbol);
                } else if (elemSort == PLCModifierEnum.Sort.INT) {
                    PLCVariable exprVar = (PLCVariable) elemSymbol;
                    if(!exprVar.getIfConst()){
                        throw new PLCSemanticException("const variable expected : " + caseListElemContext.getText());
                    }
                    caseList.add(exprVar);
                }else{
                    throw new PLCSemanticException("type mismatch : " + caseListElemContext.getText());
                }
            }catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
        return caseList;
    }
}