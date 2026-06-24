package PLCTranslator.TranslateType.Method_prototype;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateMethod_prototype {
    //接口参数语句
    ArrayList<String> interfaceParaSentences = new ArrayList<>();

    public String translateNode(PLCSTPARSERParser.Method_prototypeContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        //方法原型名称
        String MethodProtoTypeName = ctx.method_name().getText();
        //方法原型返回类型
        String methodProtoReTypeName = "void";
        if(ctx.data_type_access()!=null) {
            PLCTypeDeclSymbol methodProtoReType = (PLCTypeDeclSymbol) PLCTranslatorNew.properties.get(ctx.data_type_access()).get(0);
            methodProtoReTypeName = methodProtoReType.getRuntimeTypeName();
        }
        //********************************************************方法原型类声明*******************************************
        sb.append("\n\tclass "+MethodProtoTypeName +": public METHODPROTO {");

        //******************************************************方法原型参数变量翻译****************************************
        for (PLCSTPARSERParser.Io_var_declsContext io_var_decl : ctx.io_var_decls()) {
            ArrayList<PLCSymbol> ioList = PLCTranslatorNew.properties.get(io_var_decl);
            for (PLCSymbol symbol : ioList) {
                PLCVariable interfaceIOVarSymbol = (PLCVariable) symbol;
                //接口参数语句
                this.interfaceParaSentences.add(interfaceIOVarSymbol.getRuntimeTypeName() + interfaceIOVarSymbol.getRuntimeName() +"= new "
                        +interfaceIOVarSymbol.getRuntimeTypeName()+"("+interfaceIOVarSymbol.getAssignVar()+")");
            }
        }

        //******************************************************接口类中纯虚函数翻译****************************************
        sb.append("\n\t\tvirtual "+methodProtoReTypeName+ " callFunc(");
        sb.append(MethodProtoTypeName+"* meth");
        for (String interfaceParaSentence : this.interfaceParaSentences) {
            sb.append(","+interfaceParaSentence);
        }
        sb.append(") = 0;\n");
        sb.append("\t};");
        return sb.toString();
    }
}
