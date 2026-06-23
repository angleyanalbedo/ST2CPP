package PLCTranslator.TranslateType.Class_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

public class TranslateClass_decl {

    //存储类内变量指针语句，将其信息输出到method中
    public static ArrayList<PLCVariable> classVariableList = new ArrayList<>();

    //类内类语句
    ArrayList<String> conSentences = new ArrayList<>();

    packageFactory pFactory = new packageFactory();

    public ArrayList<String> translateNode(PLCSTPARSERParser.Class_declContext ctx, PLCTranslatorNew translatorNew){
        //*******************************************************翻译类中函数类********************************************
        for (PLCSTPARSERParser.Method_declContext method_declContext : ctx.method_decl()) {
            //翻译函数体
            translatorNew.visit(method_declContext);
        }
        //*****************************************************翻译类中变量声明********************************************
        PLCClassDeclSymbol classDeclSymbol = (PLCClassDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);

        packageCreateClassVariable(classDeclSymbol);

        //*****************************************************翻译类的声明***********************************************
        writeTarget("\nclass " +classDeclSymbol.getName()+" : public CLASS{");
        writeTarget("\npublic:");
        writeTarget("\n\t"+classDeclSymbol.getName()
                +"(int instanceId, PLC::varMap *vMap) : CLASS(instanceId, vMap) {");
        for (String conSentence : this.conSentences) {
            writeTarget("\n\t\t"+conSentence);
        }
        writeTarget("\n\t}");
        writeTarget("\n};");

        return null;
    }

    /**
     * @describe 组装类创建普通变量语句
     * @param classDeclSymbol 类变量声明列表
     */
    public void packageCreateClassVariable(PLCClassDeclSymbol classDeclSymbol){
        //组装创建变量语句
        for (PLCVariable value : classDeclSymbol.getVariableMap().values()) {
            this.conSentences.add(pFactory.packageCreateClassVariableSentences(value));
        }
        for (PLCMethodDeclSymbol method : classDeclSymbol.getMethods()) {
            this.conSentences.add(pFactory.packageCreateClassMethodSentences(method));
        }
    }



}
