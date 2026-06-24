package PLCTranslator.TranslateType.Func_decl;

import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

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

    public String translateNode(PLCSTPARSERParser.Func_declContext ctx, PLCTranslatorNew translatorNew) {
        boolean isFlat = translatorNew.codeGen instanceof FlatCodeGenerator;
        StringBuilder sb = new StringBuilder();
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
            returnSentence1 = "new " + funcReType +"(getFuncReturn<"+funcReType+"*>)";
//            returnSentence2 = "auto re =";
            //returnSentence3 = "return this->getFuncReturnVar<"+funcReType+">();";
        }

        //***************************************获取变量段变量信息组装所需语句**********************************************
        if(ctx.io_var_decls().size() != 0) {
            this.packageIOSentences((ArrayList<PLCSTPARSERParser.Io_var_declsContext>) ctx.io_var_decls(), isFlat);
        }
        if(ctx.func_var_decls().size() != 0) {
            this.packageFuncVarSentences((ArrayList<PLCSTPARSERParser.Func_var_declsContext>)  ctx.func_var_decls(), isFlat);
        }
        if(ctx.temp_var_decls().size() != 0) {
            this.packageTempVarSentences((ArrayList<PLCSTPARSERParser.Temp_var_declsContext>) ctx.temp_var_decls(), isFlat);
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

        if (isFlat) {
            // Flat 模式：生成原生 C++ 函数
            StringBuilder params = new StringBuilder();
            if (this.funcParaSentences.size() > 0) {
                params.append(this.funcParaSentences.get(0));
                for (int t = 1; t < this.funcParaSentences.size(); t++) {
                    params.append(", ").append(this.funcParaSentences.get(t));
                }
            }
            sb.append(translatorNew.codeGen.emitFuncDeclBegin(funcName, returnValueType, params.toString()));
            // 局部变量声明（func_var, temp_var）
            for (String initSentence : this.funcCallInitSentences) {
                sb.append(initSentence);
            }
            // 函数体
            String result = translatorNew.visit(ctx.func_body());
            sb.append(result);
            // 输出返回
            for (String funcCallOutputSentence : this.funcCallOutputSentences) {
                sb.append(funcCallOutputSentence);
            }
            sb.append(translatorNew.codeGen.emitFuncDeclEnd());
        } else {
            // OOP 模式：保持原有逻辑
            //函数类占位符
            sb.append("\nclass "+funcName+": public PLC_Function<"+returnValueType+">{");
            sb.append("\npublic:");

            //*************************************************组装函数类的构造函数*********************************************
            sb.append("\n\t"+funcName + "(int funcID, varMap* vMap) : PLC_Function(funcID, vMap){");
            if(ctx.data_type_access()!=null){
                sb.append("\n\t\t"+"this->addReturnVar<"+funcReType+">();");
                sb.append("\n\t\t"+"this->returnValue = new "+funcReType+"();");
            }
            for (String conSentence : this.conSentences) {
                sb.append("\n\t\t"+conSentence);
            }
            sb.append("\n\t}");

            //**************************************************组装函数执行接口***********************************************
            String callFuncReturnType = "void";
            sb.append("\n\t"+callFuncReturnType+" funcExecute(");
            //函数执行参数翻译
            if(this.funcParaSentences.size()>0){
                sb.append(this.funcParaSentences.get(0));
            }
            for(int t = 1;t<this.funcParaSentences.size();t++){
                sb.append(","+this.funcParaSentences.get(t));
            }
            sb.append("){");
            //输入参数变量传值初始化
            for (String funcCallInitSentence : this.funcCallInitSentences) {
                sb.append("\n\t\t"+funcCallInitSentence);
            }

            //访问函数操作体
            String result = translatorNew.visit(ctx.func_body());
            sb.append(result);

            //函数输出返回
            for (String funcCallOutputSentence : this.funcCallOutputSentences) {
                sb.append("\n\t\t"+funcCallOutputSentence);
            }
            sb.append("\n\t}");

            //*************************************************组装变量重置接口************************************************
//        sb.append("\n\tvoid resetValue(){");
//        for (String resetSentence : this.resetSentences) {
//            sb.append("\n\t\t"+resetSentence);
//        }
//        sb.append("\n\t}");

            //*************************************************组装函数调用接口************************************************
            String callFuncReturn ="void";
            if(ctx.data_type_access()!=null){
                callFuncReturn = funcReType+"*";
            }
            sb.append("\n\t"+callFuncReturn+" callFunc(");
            if(this.funcParaSentences.size()>0){
                sb.append(this.funcParaSentences.get(0));
            }
            for(int t = 1;t<this.funcParaSentences.size();t++){
                sb.append(","+this.funcParaSentences.get(t));
            }
            sb.append("){");

            sb.append("\n\t\t"+"funcExecute(");

            if(this.callFuncParaSentences.size()>0){
                sb.append(this.callFuncParaSentences.get(0));
            }
            for(int t =1; t<this.callFuncParaSentences.size(); t++){
                sb.append(","+this.callFuncParaSentences.get(t));
            }
            sb.append(");");

//        sb.append("\n\t\tresetValue();");

            sb.append("\n\t\treturn "+returnSentence1+";");

            sb.append("\n\t}");

            sb.append("\n};");

            //将该函数实例化添加到总表下
            funcInitSentences.add(pFactory.packageAddVarToMapSentences(String.valueOf(funcSymbol.getSymbolId()),
                    funcSymbol.getName()));
        }

        return sb.toString();
    }

    /**
     * @describe 组装和输入输出变量相关的语句
     * @param ioList io变量段结点
     * @param isFlat 是否为 Flat 模式
     */
    void packageIOSentences(ArrayList<PLCSTPARSERParser.Io_var_declsContext> ioList, boolean isFlat) {
        for (PLCSTPARSERParser.Io_var_declsContext io_var_declsContext : ioList) {
            ArrayList<PLCSymbol> ioVarList = properties.get(io_var_declsContext);
            for (PLCSymbol symbol : ioVarList) {
                //类型转换
                PLCVariable tempSymbol = (PLCVariable) symbol;
                //获取变量段信息
                PLCModifierEnum.VarSections sections = tempSymbol.getVarSections();

                if (isFlat) {
                    // Flat 模式：参数使用原生 C++ 类型
                    String nativeType = tempSymbol.getRuntimeTypeName();
                    // 映射到原生类型（去掉 PLC_ 前缀等）
                    nativeType = mapToNativeType(nativeType);
                    if(sections == PLCModifierEnum.VarSections.VAR_INPUT){
                        this.funcParaSentences.add(nativeType + " " + tempSymbol.getName());
                        this.callFuncParaSentences.add(tempSymbol.getName());
                    }else if(sections == PLCModifierEnum.VarSections.VAR_IN_OUT || sections == PLCModifierEnum.VarSections.VAR_OUTPUT){
                        this.funcParaSentences.add(nativeType + "& " + tempSymbol.getName());
                        this.callFuncParaSentences.add(tempSymbol.getName());
                        // 输出返回：直接赋值
                        this.funcCallOutputSentences.add("\n\t\t" + tempSymbol.getName() + " = " + tempSymbol.getName() + ";");
                    }
                } else {
                    // OOP 模式：保持原有逻辑
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
    }

    void packageFuncVarSentences(ArrayList<PLCSTPARSERParser.Func_var_declsContext> funcVarList, boolean isFlat){
        for (PLCSTPARSERParser.Func_var_declsContext func_var_declsContext : funcVarList) {
            ArrayList<PLCSymbol> ioVarList = properties.get(func_var_declsContext);
            for (PLCSymbol symbol : ioVarList) {
                PLCVariable tempSymbol = (PLCVariable) symbol;
                if (isFlat) {
                    // Flat 模式：局部变量声明为原生 C++ 变量
                    String nativeType = mapToNativeType(tempSymbol.getRuntimeTypeName());
                    this.funcCallInitSentences.add("\n\t" + nativeType + " " + tempSymbol.getName() + " = " + tempSymbol.getAssignVar() + ";");
                } else {
                    // OOP 模式：保持原有逻辑
                    //在构造函数中将变量加入map中
                    this.conSentences.add(pFactory.packageFuncVarMapAddSentences(tempSymbol));
                    //组装变量重置语句
                    this.resetSentences.add(pFactory.packageFuncVarResetSentences(tempSymbol));
                    //临时变量组装
                    this.funcCallInitSentences.add(pFactory.packageFuncParaLocalScopeSymbolSentences(tempSymbol));
                }
            }
        }
    }

    void packageTempVarSentences(ArrayList<PLCSTPARSERParser.Temp_var_declsContext> tempVarList, boolean isFlat){
        for (PLCSTPARSERParser.Temp_var_declsContext temp_var_declsContext : tempVarList) {
            ArrayList<PLCSymbol> ioVarList = properties.get(temp_var_declsContext);
            for (PLCSymbol symbol : ioVarList) {
                PLCVariable tempSymbol = (PLCVariable) symbol;
                if (isFlat) {
                    // Flat 模式：临时变量声明为原生 C++ 变量
                    String nativeType = mapToNativeType(tempSymbol.getRuntimeTypeName());
                    this.funcCallInitSentences.add("\n\t" + nativeType + " " + tempSymbol.getName() + " = " + tempSymbol.getAssignVar() + ";");
                } else {
                    // OOP 模式：保持原有逻辑
                    //在构造函数中将变量加入map中
                    this.conSentences.add(pFactory.packageFuncVarMapAddSentences(tempSymbol));
                    //组装变量重置语句
                    this.resetSentences.add(pFactory.packageFuncVarResetSentences(tempSymbol));
                }
            }
        }
    }

    /**
     * 将运行时类型名映射为原生 C++ 类型名
     */
    private String mapToNativeType(String runtimeTypeName) {
        if (runtimeTypeName == null) return "int";
        switch (runtimeTypeName) {
            case "PLC_SINT_Value": return "SINT";
            case "PLC_INT_Value": return "INT";
            case "PLC_DINT_Value": return "DINT";
            case "PLC_LINT_Value": return "LINT";
            case "PLC_Real_Value": return "REAL";
            case "PLC_LReal_Value": return "LREAL";
            case "PLC_Bool_Value": return "BOOL";
            case "PLC_String_Value": return "STRING";
            default:
                // 如果已经是原生类型或未知，直接返回
                return runtimeTypeName;
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
