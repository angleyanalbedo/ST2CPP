package PLCTranslator.TranslateType.Func_decl;

import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;
import static PLCTranslator.PLCTranslatorNew.properties;
import static PLCTranslator.TranslateType.Startpoint.TranslateStartpoint.funcInitSentences;

public class TranslateFunc_decl {
    //函数类的构造函数语句
    ArrayList<String> conSentences = new ArrayList<>();

    //函数类中参数重置语句
    ArrayList<String> resetSentences = new ArrayList<>();

    //函数调用接口参数
    ArrayList<String> funcParaSentences = new ArrayList<>();

    //函数调用接口内变量初始化
    ArrayList<String> funcCallInitSentences = new ArrayList<>();

    //函数调用接口内返回输出语句
    ArrayList<String> funcCallOutputSentences = new ArrayList<>();

    //组装方法类
    packageFactory pFactory = new packageFactory();

    //函数调用参数语句
    ArrayList<String> callFuncParaSentences = new ArrayList<>();

    public ArrayList<String> translateNode(PLCSTPARSERParser.Func_declContext ctx, PLCTranslatorNew translatorNew) {
        //获取函数符号
        PLCFCDeclSymbol funcSymbol = (PLCFCDeclSymbol) properties.get(ctx).get(0);
        //函数返回语句信息
        String returnSentence1 ="NULL";

        String returnSentence2 ="";

        String returnSentence3 ="";
        //函数返回类型
        String funcReType ="void";
        if(ctx.data_type_access()!=null) {
            funcReType = PLCTotalSymbolTable.getTypeByTypeID(funcSymbol.getReturnTypeId()).getRuntimeName();
            returnSentence1 = "new " + funcReType +"(getFuncReturn<"+funcReType+"*>())";
//            returnSentence2 = "auto re =";
            //returnSentence3 = "return this->getFuncReturnVar<"+funcReType+">();";
        }

        //***************************************获取变量段变量信息组装所需语句**********************************************
        if(ctx.io_var_decls().size() != 0) {
            this.packageIOSentences((ArrayList<PLCSTPARSERParser.Io_var_declsContext>) ctx.io_var_decls());
        }
        if(ctx.func_var_decls().size() != 0) {
            this.packageFuncVarSentences((ArrayList<PLCSTPARSERParser.Func_var_declsContext>)  ctx.func_var_decls());
        }
        if(ctx.temp_var_decls().size() != 0) {
            this.packageTempVarSentences((ArrayList<PLCSTPARSERParser.Temp_var_declsContext>) ctx.temp_var_decls());
        }

        //****************************************************翻译函数声明************************************************
        //获取函数名
        String funcName = ctx.derived_func_name().getText();

        String returnValueType;
        if(funcSymbol.getReturnTypeId() == -1){
            returnValueType = "void";
        }else{
            returnValueType = PLCTotalSymbolTable.getTypeByTypeID(funcSymbol.getReturnTypeId()).getName();
        }
        //函数类占位符
        writeTarget("\nclass "+funcName+": public PLC_Function<"+returnValueType+">{");
        writeTarget("\npublic:");

        //*************************************************组装函数类的构造函数*********************************************
        writeTarget("\n\t"+funcName + "(int funcID, varMap* vMap) : PLC_Function(funcID, vMap){");
        if(ctx.data_type_access()!=null){
            writeTarget("\n\t\t"+"this->addReturnVar<"+funcReType+">();");
            writeTarget("\n\t\t"+"this->returnValue = new "+funcReType+"();");
        }
        for (String conSentence : this.conSentences) {
            writeTarget("\n\t\t"+conSentence);
        }
        writeTarget("\n\t}");

        //**************************************************组装函数执行接口***********************************************
        String callFuncReturnType = "void";
        writeTarget("\n\t"+callFuncReturnType+" funcExecute(");
        //函数执行参数翻译
        if(this.funcParaSentences.size()>0){
            writeTarget(this.funcParaSentences.get(0));
        }
        for(int t = 1;t<this.funcParaSentences.size();t++){
            writeTarget(","+this.funcParaSentences.get(t));
        }
        writeTarget("){");
        //输入参数变量传值初始化
        for (String funcCallInitSentence : this.funcCallInitSentences) {
            writeTarget("\n\t\t"+funcCallInitSentence);
        }

        //访问函数操作体
        translatorNew.visit(ctx.func_body());

        //函数输出返回
        for (String funcCallOutputSentence : this.funcCallOutputSentences) {
            writeTarget("\n\t\t"+funcCallOutputSentence);
        }
        writeTarget("\n\t}");

        //*************************************************组装变量重置接口************************************************
//        writeTarget("\n\tvoid resetValue(){");
//        for (String resetSentence : this.resetSentences) {
//            writeTarget("\n\t\t"+resetSentence);
//        }
//        writeTarget("\n\t}");

        //*************************************************组装函数调用接口************************************************
        String callFuncReturn ="void";
        if(ctx.data_type_access()!=null){
            callFuncReturn = funcReType+"*";
        }
        writeTarget("\n\t"+callFuncReturn+" callFunc(");
        if(this.funcParaSentences.size()>0){
            writeTarget(this.funcParaSentences.get(0));
        }
        for(int t = 1;t<this.funcParaSentences.size();t++){
            writeTarget(","+this.funcParaSentences.get(t));
        }
        writeTarget("){");

        writeTarget("\n\t\t"+"funcExecute(");

        if(this.callFuncParaSentences.size()>0){
            writeTarget(this.callFuncParaSentences.get(0));
        }
        for(int t =1; t<this.callFuncParaSentences.size(); t++){
            writeTarget(","+this.callFuncParaSentences.get(t));
        }
        writeTarget(");");

//        writeTarget("\n\t\tresetValue();");

        writeTarget("\n\t\treturn "+returnSentence1+";");

        writeTarget("\n\t}");

        writeTarget("\n};");

        //将该函数实例化添加到总表下
        funcInitSentences.add(pFactory.packageAddVarToMapSentences(String.valueOf(funcSymbol.getSymbolId()),
                funcSymbol.getName()));

        return null;
    }

    /**
     * @describe 组装和输入输出变量相关的语句
     * @param ioList io变量段结点
     */
    void packageIOSentences(ArrayList<PLCSTPARSERParser.Io_var_declsContext> ioList) {
        for (PLCSTPARSERParser.Io_var_declsContext io_var_declsContext : ioList) {
            ArrayList<PLCSymbol> ioVarList = properties.get(io_var_declsContext);
            for (PLCSymbol symbol : ioVarList) {
                //类型转换
                PLCVariable tempSymbol = (PLCVariable) symbol;
                //获取变量段信息
                PLCModifierEnum.VarSections sections = tempSymbol.getVarSections();

                //在构造函数中将变量加入map中
                this.conSentences.add(pFactory.packageFuncVarMapAddSentences(tempSymbol));
//                //组装变量重置语句
//                this.resetSentences.add(pFactory.packageFuncVarResetSentences(tempSymbol));
                //组装执行函数参数
                this.callFuncParaSentences.add(tempSymbol.getName());
                //根据变量段类型的不同来组装相应的语句
                if(sections == PLCModifierEnum.VarSections.VAR_INPUT){
                    //组装调用参数语句
                    this.funcParaSentences.add(pFactory.packageFuncParaSentences(tempSymbol));

                    //组装调用参数初始化语句
                    this.funcCallInitSentences.add(pFactory.packageFuncParaLocalScopeSymbolSentences(tempSymbol));
                }else if(sections == PLCModifierEnum.VarSections.VAR_IN_OUT){
                    //组装调用参数语句
                    this.funcParaSentences.add(pFactory.packageFuncInoutParaSentences(tempSymbol));
                    //组装调用参数初始化语句
                    this.funcCallInitSentences.add(pFactory.packageFuncParaLocalScopeSymbolSentences(tempSymbol));
                    //组装输出返回语句
                    this.funcCallOutputSentences.add(pFactory.packageFuncOutputSentences(tempSymbol));
                }else{
                    //组装调用参数语句
                    this.funcParaSentences.add(pFactory.packageFuncInoutParaSentences(tempSymbol));
                    //组装输出返回语句
                    this.funcCallOutputSentences.add(pFactory.packageFuncOutputSentences(tempSymbol));
                }
            }
        }
    }
    void packageFuncVarSentences(ArrayList<PLCSTPARSERParser.Func_var_declsContext> funcVarList){
        for (PLCSTPARSERParser.Func_var_declsContext func_var_declsContext : funcVarList) {
            ArrayList<PLCSymbol> ioVarList = properties.get(func_var_declsContext);
            for (PLCSymbol symbol : ioVarList) {
                PLCVariable tempSymbol = (PLCVariable) symbol;
                //在构造函数中将变量加入map中
                this.conSentences.add(pFactory.packageFuncVarMapAddSentences(tempSymbol));
                //组装变量重置语句
                this.resetSentences.add(pFactory.packageFuncVarResetSentences(tempSymbol));
                //临时变量组装
                this.funcCallInitSentences.add(pFactory.packageFuncParaLocalScopeSymbolSentences(tempSymbol));
            }
        }
    }

    void packageTempVarSentences(ArrayList<PLCSTPARSERParser.Temp_var_declsContext> tempVarList){
        for (PLCSTPARSERParser.Temp_var_declsContext temp_var_declsContext : tempVarList) {
            ArrayList<PLCSymbol> ioVarList = properties.get(temp_var_declsContext);
            for (PLCSymbol symbol : ioVarList) {
                PLCVariable tempSymbol = (PLCVariable) symbol;
                //在构造函数中将变量加入map中
                this.conSentences.add(pFactory.packageFuncVarMapAddSentences(tempSymbol));
                //组装变量重置语句
                this.resetSentences.add(pFactory.packageFuncVarResetSentences(tempSymbol));
            }
        }
    }

//    String packageFuncReturnSentences(PLCSTPARSERParser.Func_declContext funcDeclContext){
//
//        PLCFCDeclSymbol funcSymbol = (PLCFCDeclSymbol) properties.get(funcDeclContext).get(0);
//
//        if(funcDeclContext.data_type_access()!=null) {
//            PLCTypeDeclSymbol typeByTypeID = PLCTotalSymbolTable.getTypeByTypeID(funcSymbol.getReturnTypeId());
//            return "return ::PLC::RFM->getSymbolByID<" + typeByTypeID.getRuntimeName() + "*>"
//                    + "(" + String.valueOf(funcSymbol.symbolId) + ");";
//        }else{
//            return "return NULL;";
//        }
//    }


}
