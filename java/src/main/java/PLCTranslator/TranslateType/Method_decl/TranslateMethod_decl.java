package PLCTranslator.TranslateType.Method_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.Map;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;
import static PLCTranslator.TranslateType.Class_decl.TranslateClass_decl.classVariableList;

public class TranslateMethod_decl {
    //函数类的构造函数语句
    ArrayList<String> conSentences = new ArrayList<>();

    //函数类中参数重置语句
    ArrayList<String> resetSentences = new ArrayList<>();

    ArrayList<String> callFuncParaSentences = new ArrayList<>();

    //函数return返回语句
    int returnOrNot = 0;

    //函数调用接口参数
    ArrayList<String> funcParaSentences = new ArrayList<>();

    //函数调用接口内变量初始化
    ArrayList<String> funcCallInitSentences = new ArrayList<>();

    //函数调用接口内返回输出语句
    ArrayList<String> funcCallOutputSentences = new ArrayList<>();

    packageFactory pFactory = new packageFactory();

    public ArrayList<String> translateNode(PLCSTPARSERParser.Method_declContext ctx, PLCTranslatorNew translatorNew) {
        if(ctx.data_type_access()!=null){
            this.returnOrNot = 1;
        }

        //***********************************************组装变量段语句****************************************************
        PLCMethodDeclSymbol methodDeclSymbol = (PLCMethodDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);
        packageMethodIOVarSentences(methodDeclSymbol.getVariableMap());

        //************************************************翻译函数声明****************************************************
        writeTarget("\nclass "+methodDeclSymbol.getName()+": public METHOD{");
        writeTarget("\npublic:");

        //***********************************************翻译方法构造函数**************************************************
        writeTarget("\n\t"+methodDeclSymbol.getName()+"(int methodID, std::string methodName, CLASS* classP)");
        writeTarget(":METHOD(methodID, std::move(methodName), classP){");
        for (String conSentence : this.conSentences) {
            writeTarget("\n\t\t"+conSentence);
        }
        writeTarget("\n\t}");

        //***********************************************翻译方法执行接口**************************************************
        if(returnOrNot == 1){
            writeTarget("\n\tauto funcExecute(");
        }else{
            writeTarget("\n\tvoid funcExecute(");
        }

        //函数执行接口参数翻译
        if(!this.funcParaSentences.isEmpty()){
            writeTarget(this.funcParaSentences.get(0));
            for(int t = 1; t<this.funcParaSentences.size(); t++){
                writeTarget(","+this.funcParaSentences.get(t));
            }
        }
        writeTarget("){");

        //传递输入参数值
        for (String funcCallInitSentence : this.funcCallInitSentences) {
            writeTarget("\n\t\t"+funcCallInitSentence);
        }

        //翻译函数执行内容
        translatorNew.visit(ctx.func_body());

        //将输出参数值赋值返回
        for (String funcCallOutputSentence : this.funcCallOutputSentences) {
            writeTarget(funcCallOutputSentence);
        }
        writeTarget("\n\t}");

        //*******************************************翻译函数内值重置接口***************************************************
        writeTarget("\n\tvoid resetValue(){");
        for (String resetSentence : this.resetSentences) {
            writeTarget("\n\t\t"+resetSentence);
        }
        writeTarget("\n\t}");

        //*********************************************翻译函数调用接口****************************************************
        if(returnOrNot == 1) {
            writeTarget("\n\t\tauto callFunc(");
        }else{
            writeTarget("\n\t\t void callFunc(");
        }

        //函数调用参数翻译
        if(!this.funcParaSentences.isEmpty()){
            writeTarget(this.funcParaSentences.get(0));
            for(int t = 1; t<this.funcParaSentences.size(); t++){
                writeTarget(","+this.funcParaSentences.get(t));
            }
        }

        writeTarget("){");
        writeTarget("\n\t\tfuncExecute(");
        if(!this.callFuncParaSentences.isEmpty()){
            writeTarget(this.callFuncParaSentences.get(0));
            for(int t = 1; t<this.callFuncParaSentences.size(); t++){
                writeTarget(","+this.callFuncParaSentences.get(t));
            }
        }
        writeTarget(");");
        writeTarget("\n\t\tresetValue();");
        writeTarget("\n\t}");
        writeTarget("};");
        return null;
    }

    void packageMethodIOVarSentences(Map<String, PLCVariable> valueMap){

        for (PLCVariable value : valueMap.values()) {
            //组装构造函数内变量创建语句
            this.conSentences.add(pFactory.packageCreateMethodVariableSentences(value));

            //组装变量重置语句
            this.resetSentences.add(pFactory.packageFuncVarResetSentences(value));

            if(value.getVarSections() == PLCModifierEnum.VarSections.VAR_INPUT){
                //组装执行函数参数
                this.callFuncParaSentences.add(value.getName());

                //组装调用参数语句
                this.funcParaSentences.add(pFactory.packageFuncParaSentences(value));

                //组装调用参数初始化语句
                this.funcCallInitSentences.add(pFactory.packageFuncParaInitSentences(value));
            }else if(value.getVarSections() == PLCModifierEnum.VarSections.VAR_OUTPUT){
                //组装执行函数参数
                this.callFuncParaSentences.add(value.getName());
                //组装调用参数语句
                this.funcParaSentences.add(pFactory.packageFuncInoutParaSentences(value));
                //组装输出返回语句
                this.funcCallOutputSentences.add(pFactory.packageFuncOutputSentences(value));
            }else if(value.getVarSections() == PLCModifierEnum.VarSections.VAR_IN_OUT){
                //组装执行函数参数
                this.callFuncParaSentences.add(value.getName());
                //组装调用参数语句
                this.funcParaSentences.add(pFactory.packageFuncInoutParaSentences(value));
                //组装调用参数初始化语句
                this.funcCallInitSentences.add(pFactory.packageFuncInoutParaInitSentences(value));
                //组装输出返回语句
                this.funcCallOutputSentences.add(pFactory.packageFuncOutputSentences(value));
            }
        }
    }

}
