package PLCTranslator.TranslateType;

import PLCSymbolAndScope.PLCSymbols.PLCMethodDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class packageFactory {
    //RUNTIME类内构造函数

//    /**
//     * @describe 组装runtime内特殊翻译映射规范的类内构造函数初始化语句
//     * @param runtimeVarName runtime类内指针变量名
//     * @param runtimeTypeName runtime类内指针变量类型名
//     * @param assignValue runtime类型指针变量名
//     * @return this-><varName> = new <varType>（<varAssign>）;
//     */
//    public String packageConInitSentences(String runtimeVarName, String runtimeTypeName, String assignValue){
//        return "this->" + runtimeVarName +"= new "+runtimeTypeName+"("+assignValue+");";
//    }

    /**
     * @describe 去掉getAssign句子中首次出现的星号
     * @return
     */
    public String deleteStarSymbol(String inputSentences){
        return inputSentences.substring(2, inputSentences.length()-1);
    }

    /**
     * @describe 组装函数内变量添加语句
     * @param tempSymbol 变量符号对象
     * @return 返回符号对象添加语句
     */
    public String packageFuncVarMapAddSentences(PLCVariable tempSymbol){
        return "::PLC::RFM->addVarToVarMap("+String.valueOf(tempSymbol.symbolId)+",createFuncVariable<"+tempSymbol.getRuntimeTypeName()
                +">("+tempSymbol.getSymbolId()+","+deleteStarSymbol(tempSymbol.getAssignVar())+"));";
    }

    public String packageFuncVarResetSentences(PLCVariable tempSymbol){
        return "::PLC::RFM->getSymbolByID<"+tempSymbol.getRuntimeTypeName()+"*>"+"("+String.valueOf(tempSymbol.symbolId)+")->setValue("
                +tempSymbol.getAssignVar()+");";
    }

    /**
     * @describe 组装函数调用参数接口语句
     * @param tempSymbol 初始化参数符号对象
     * @return 函数调用接口参数语句
     */
    public String packageFuncParaSentences(PLCVariable tempSymbol){
        return tempSymbol.getRuntimeTypeName()+"*"+tempSymbol.getName();
    }

    /**
     * @describe 组装局部变量
     */
    public String packageFuncParaLocalScopeSymbolSentences(PLCVariable tempVariable){
        String typeName = tempVariable.getRuntimeTypeName();
        return "auto* "+ tempVariable.getName()+"_t" + " = " + "new "+ typeName
                +"(::PLC::RFM->getSymbolByID<"+typeName+"*>("+tempVariable.getSymbolId()+"));"+
                "\n\t\t*"+tempVariable.getName()+"_t = *"+tempVariable.getName()+";\n";
    }

    /**
     * @describe  组装函数参数初始化语句
     * @param tempSymbol 初始化参数符号对象
     * @return 返回初始化语句
     */
    public String packageFuncParaInitSentences(PLCVariable tempSymbol){
        return "*::PLC::RFM->getSymbolByID<"+tempSymbol.getRuntimeTypeName()+"*>"+"("+String.valueOf(tempSymbol.symbolId)
                +")="+ "*" +tempSymbol.getName()+";";
    }

    /**
     * @describe 组装函数输入输出参数初始化语句
     * @param tempSymbol 参数符号对象
     *
     */
    public String packageFuncInoutParaInitSentences(PLCVariable tempSymbol){
        return "*::PLC::RFM->getSymbolByID<"+tempSymbol.getRuntimeTypeName()+"*>"+"("+String.valueOf(tempSymbol.symbolId)
                +")="+ "*::PLC::RFM->getSymbolByID<"+tempSymbol.getRuntimeTypeName()+"*>"+"("+tempSymbol.getAssignVar()+");";
    }

    /**
     * @describe 组装函数输入输出调用接口参数
     */
    public String packageFuncInoutParaSentences(PLCVariable tempSymbol){
        return "int "+tempSymbol.getName();
    }

    /**
     * @describe 组装函数执行中输出返回
     * @param tempSymbol 输出参数符号
     * @return 返回相应的返回输出语句
     */
    public String packageFuncOutputSentences(PLCVariable tempSymbol){
        return "*::PLC::RFM->getSymbolByID<"+tempSymbol.getRuntimeTypeName()+"*>("+tempSymbol.getName()+")="
                +"*::PLC::RFM->getSymbolByID<"+tempSymbol.getRuntimeTypeName()+"*>"
                +"("+String.valueOf(tempSymbol.symbolId)+");";
    }

    /**
     * @describe 将对象加入总表中
     * @param symbolID 符号对象ID
     * @param symbolTypeName 符号对象类名
     * @return 返回添加到对象总表的语句
     */
    public String packageAddVarToMapSentences(String symbolID, String symbolTypeName){
        return "::PLC::RFM->addVarToVarMap("+symbolID+", new "+symbolTypeName+"("+symbolID+", RFM" +"));";
    }

    /**
     * @describe
     * @param tempSymbol 方法符号对象
     * @return 返回类构造函数中创建类方法对象的方法
     */
    public String packageClassCreateMethodSentences(PLCMethodDeclSymbol tempSymbol){
        return "createMethod<"+tempSymbol.getName()+">"+"(\"" +tempSymbol.getName()+"\","
                +tempSymbol.getSymbolId()+",this);";
    }

    /**\
     * @describe 组装类内创建变量语句
     * @param plcVariable 类内变量符号对象
     * @return 返回类内创建变量语句
     */
    public String packageCreateClassVariableSentences(PLCVariable plcVariable){
        return "createClassVariable<"+plcVariable.getRuntimeTypeName()+">(\""+plcVariable.getName()+"\","
                +plcVariable.getSymbolId()+", new "+plcVariable.getRuntimeTypeName()+"("+plcVariable.getAssignVar()+"));";
    }

    /**
     * @describe 组装类内函数对象创建语句
     * @param plcMethodDeclSymbol 类内函数符号对象
     * @return 返回创建方法对象语句
     */
    public String packageCreateClassMethodSentences(PLCMethodDeclSymbol plcMethodDeclSymbol){
        return "createMethod<"+plcMethodDeclSymbol.getRuntimeTypeName()+">(\""+plcMethodDeclSymbol.getName()+"\","
                +plcMethodDeclSymbol.getSymbolId()+", this);";
    }

    /**
     * @describe 组装类内函数变量创建语句
     * @param plcVariable 变量对象符号
     * @return 返回函数内变量创建语句
     */
    public String packageCreateMethodVariableSentences(PLCVariable plcVariable){
        return "createMethodVariable("+plcVariable.getSymbolId()+", " +plcVariable.getAssignVar()+");";
    }

    /**
     * @describe 组装根据ID从RFM中获取变量对象语句
     * @param symbolType
     * @param symbolID
     * @return 返回get语句
     */
    public String packageGetSymbolByIDSentences(String symbolType, int symbolID){
        return "::PLC::PFM->getSymbolByID<"+symbolType+">("+symbolID+")";
    }

    public String packageFuncMethodParaSentences(String runtimeVarName, String runtimeTypeName, String assignValue){
        return runtimeTypeName +"*runtimeTypeName"+runtimeVarName + "= new "+runtimeTypeName+"("+assignValue+")";
    }

    public String packageFuncMethodParaSentences(PLCVariable varSymbol){
        return varSymbol.getRuntimeTypeName()+"*"+varSymbol.getName();
    }

    public String packageAssignSentences(String runtimeVarName, String expressionSentences){
        return runtimeVarName+"="+expressionSentences+";";
    }

    public String packagePROGVarDeclSentences(String runtimeName, String runtimeTypeName, String assignValues){
        return "auto* " + runtimeName +"="+"new "+runtimeTypeName
                +"("+assignValues+");";
    }

    public String packageEnumTypeDeclSentences(String typeID, String EnumName){
        return "PLC_Enum_Type<"+typeID+">* " +EnumName+"= new PLC_Enum_Type<"+typeID+"> "+"(\""+EnumName+"\",0);\n";
    }

    public String packageArrayTypeDeclSentences(String arrayName){
        return "vector<PLC_Value*>* " + arrayName +" = new vector<PLC_Value*>();";
    }

    public String packageArrayTypeInitSentences(String varName){
        return "vector<PLC_Value*>* v"+varName+" = new vector<PLC_Value*>();";
    }

    public String packageEnumElementAddSentences(String enumName,String valueType,String elementName,String assignValue){
        return enumName + "->addEnumValue(new "+valueType +"("+ assignValue + ",\"" +elementName+"\"));";
    }

    public String packageTypedefSentences(String typeName, String defName){
        return "typedef "+typeName+" "+defName+";";
    }

    public String packageArrayElementAddSentences(String assignValue, String arrayName){
        return "v"+arrayName+"->push_back("+assignValue+");";
    }

    public String packageStructTypeDecl(String structTypeID,String structName){
        return "PLC_Struct_Type<"+structTypeID+">* "+ structName+"= new "+"PLC_Struct_Type<"+structTypeID+">"+"(\""+structName+"\");";
    }

    public String packageEnumAssignSentences(){
        return null;
    }

    public String packageArrayAssignSentences(){
        return null;
    }

    public String packageStructElementAddSentences(String structName, String valueName,String assignValue){
        return structName+"->addField(0,"+"\""+valueName+"\",*(new "+valueName+"("+assignValue+")));";
    }

    public String packageCallFuncSentences(String runtimeFuncName, String runtimeFuncFieldName,String runtimeFuncPara){
        return runtimeFuncName+"funcCall("+runtimeFuncFieldName+","+runtimeFuncPara+")";
    }

    public String packageUsingNSSentences(String usingNSName){
        return "using namespace "+usingNSName+";";
    }

    public String packageExtendsSentences(String extendsClassName){
        return ", public "+extendsClassName;
    }

    public String packageImplementInterfaceSentences(String interfaceName){
        return ", public "+interfaceName;
    }
}
