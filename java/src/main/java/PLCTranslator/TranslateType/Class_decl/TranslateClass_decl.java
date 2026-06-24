package PLCTranslator.TranslateType.Class_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateClass_decl {

    //存储类内变量指针语句，将其信息输出到method中
    public static ArrayList<PLCVariable> classVariableList = new ArrayList<>();

    //类内类语句
    ArrayList<String> conSentences = new ArrayList<>();

    packageFactory pFactory = new packageFactory();

    public String translateNode(PLCSTPARSERParser.Class_declContext ctx, PLCTranslatorNew translatorNew){
        boolean isFlat = translatorNew.codeGen instanceof FlatCodeGenerator;
        StringBuilder sb = new StringBuilder();
        //*******************************************************翻译类中函数类********************************************
        for (PLCSTPARSERParser.Method_declContext method_declContext : ctx.method_decl()) {
            //翻译函数体
            String result = translatorNew.visit(method_declContext);
            sb.append(result);
        }
        //*****************************************************翻译类中变量声明********************************************
        PLCClassDeclSymbol classDeclSymbol = (PLCClassDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);

        if (isFlat) {
            // Flat 模式：类变为 C++ struct
            sb.append("\nstruct " + classDeclSymbol.getName() + " {");
            // 变量变为 struct 成员
            for (PLCVariable value : classDeclSymbol.getVariableMap().values()) {
                String nativeType = mapToNativeType(value.getRuntimeTypeName());
                sb.append("\n\t" + nativeType + " " + value.getName() + ";");
            }
            sb.append("\n};");
        } else {
            // OOP 模式：保持原有逻辑
            packageCreateClassVariable(classDeclSymbol);

            //*****************************************************翻译类的声明***********************************************
            sb.append("\nclass " +classDeclSymbol.getName()+" : public CLASS{");
            sb.append("\npublic:");
            sb.append("\n\t"+classDeclSymbol.getName()
                    +"(int instanceId, PLC::varMap *vMap) : CLASS(instanceId, vMap) {");
            for (String conSentence : this.conSentences) {
                sb.append("\n\t\t"+conSentence);
            }
            sb.append("\n\t}");
            sb.append("\n};");
        }

        return sb.toString();
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
                return runtimeTypeName;
        }
    }

}
