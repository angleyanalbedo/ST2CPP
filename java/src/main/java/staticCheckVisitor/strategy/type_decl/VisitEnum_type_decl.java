package staticCheckVisitor.strategy.type_decl;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_enum_type_decl;

@StrategyForVisit(ruleIndex = RULE_enum_type_decl)
public class VisitEnum_type_decl implements Strategy{
    /**
     * 声明enum 或者 subtype
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Enum_type_declContext ctx = (PLCSTPARSERParser.Enum_type_declContext) parserCtx;

        String enumName = ctx.enum_type_name().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, enumName);

        boolean flag = false;
        String initVar;
        PLCEnumDeclSymbol plcEnumDeclSymbol;

        if(ctx.named_spec_init() != null){
            PLCVariable variable = (PLCVariable) visitor.visit(ctx.named_spec_init()).get(0);
            plcEnumDeclSymbol = (PLCEnumDeclSymbol) variable.getDeclSymbol();
            initVar = variable.getAssignVar();
            flag = true;
        }else {
            PLCVariable variable = (PLCVariable) visitor.visit(ctx.enum_spec_init()).get(0);
            plcEnumDeclSymbol = (PLCEnumDeclSymbol) variable.getDeclSymbol();
            initVar = variable.getAssignVar();
            flag = plcEnumDeclSymbol.getTypeId() == -1;
        }

        if(flag){       //创建新类型
            //创建
            PLCEnumDeclSymbol enumDeclSymbol = new PLCEnumDeclSymbol();
            enumDeclSymbol.setName(enumName);

            //分配符号id和类型id
            enumDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
            enumDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

            //加入符号表
            currentSymbolTable.addSymbol(enumDeclSymbol);

            //入栈
            PLCScopeStack.push(enumDeclSymbol);

            //总表
            PLCTotalSymbolTable.addType(enumDeclSymbol);
            PLCTotalSymbolTable.addSymbol(enumDeclSymbol);

            enumDeclSymbol.setEnumValues(plcEnumDeclSymbol.getEnumValues());
            //枚举类型的原型默认为INT
            int protoTypeId = IDGenerator.INTID;
            if (ctx.elem_type_name() != null) {
                PLCSymbol plcSymbol = visitor.visit(ctx.elem_type_name()).get(0);
                protoTypeId = plcSymbol.getTypeId();
            }
            enumDeclSymbol.setEnumConstTypeId(protoTypeId);

            PLCTypeDeclSymbol protoType = PLCTotalSymbolTable.getTypeByTypeID(protoTypeId);
            try{
                if(protoType == null){
                    throw new PLCSemanticException("can not find type : " + ctx.elem_type_name());
                }
                //检查枚举常量的类型
                ArrayList<PLCVariable> enumValues = enumDeclSymbol.getEnumValues();
                for (PLCVariable enumValue : enumValues) {
                    if(!protoType.checkCanAssignWith(enumValue.getTypeId())){
                        throw new PLCSemanticException("type mismatch with enum const var");
                    }

                    //设置默认值
                    if(enumValue.getAssignVar().isEmpty()){
                        enumValue.setAssignVar(protoType.getInitVar());
                    }

                    //分配符号id
                    enumValue.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
                    //加入当前符号表
                    currentSymbolTable.addSymbol(enumValue);
                    //加入总表
                    PLCTotalSymbolTable.addSymbol(enumValue);
                }
            }catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }

            PLCScopeStack.pop();
            return visitor.packSymbols(enumDeclSymbol);

        }else{      //subtype
            //创建
            PLCSubtypeDeclSymbol subSymbol = new PLCSubtypeDeclSymbol();
            subSymbol.setName(enumName);

            //分配符号id和类型id
            subSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
            subSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

            //加入符号表
            currentSymbolTable.addSymbol(subSymbol);

            //总表
            PLCTotalSymbolTable.addType(subSymbol);
            PLCTotalSymbolTable.addSymbol(subSymbol);

                PLCTypeDeclSymbol prototype = PLCTotalSymbolTable.getTypeByTypeID(plcEnumDeclSymbol.getTypeId());
            subSymbol.setParentType(prototype);
            subSymbol.setInitVar(initVar);
            return visitor.packSymbols(subSymbol);
        }
    }

}
