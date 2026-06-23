package PLCTranslator.TranslateType.Method_prototype;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

public class TranslateMethod_prototype {
    //接口参数语句
    ArrayList<String> interfaceParaSentences = new ArrayList<>();

    public ArrayList<String> translateNode(PLCSTPARSERParser.Method_prototypeContext ctx, PLCTranslatorNew translatorNew){
        //方法原型名称
        String MethodProtoTypeName = ctx.method_name().getText();
        //方法原型返回类型
        String methodProtoReTypeName = "void";
        if(ctx.data_type_access()!=null) {
            PLCTypeDeclSymbol methodProtoReType = (PLCTypeDeclSymbol) PLCTranslatorNew.properties.get(ctx.data_type_access()).get(0);
            methodProtoReTypeName = methodProtoReType.getRuntimeTypeName();
        }
        //********************************************************方法原型类声明*******************************************
//        System.out.println("\tclass "+MethodProtoTypeName +": public METHODPROTO {");
        writeTarget("\n\tclass "+MethodProtoTypeName +": public METHODPROTO {");

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
//        System.out.print("\t\tvirtual "+methodProtoReTypeName+ " callFunc(");
        writeTarget("\n\t\tvirtual "+methodProtoReTypeName+ " callFunc(");
//        System.out.print(MethodProtoTypeName+"* meth");
        writeTarget(MethodProtoTypeName+"* meth");
        for (String interfaceParaSentence : this.interfaceParaSentences) {
//            System.out.print(","+interfaceParaSentence);
            writeTarget(","+interfaceParaSentence);
        }
//        System.out.print(") = 0;\n");
        writeTarget(") = 0;\n");
//        System.out.println("\t};");
        writeTarget("\t};");
        return null;
    }
}
