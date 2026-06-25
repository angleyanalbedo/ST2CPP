package PLCTranslator.TranslateType.Method_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.Map;

public class TranslateMethod_decl {
    ArrayList<String> callFuncParaSentences = new ArrayList<>();

    int returnOrNot = 0;

    //函数调用接口参数
    ArrayList<String> funcParaSentences = new ArrayList<>();

    //函数调用接口内变量初始化
    ArrayList<String> funcCallInitSentences = new ArrayList<>();

    //函数调用接口内返回输出语句
    ArrayList<String> funcCallOutputSentences = new ArrayList<>();

    public String translateNode(PLCSTPARSERParser.Method_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();
        if(ctx.data_type_access()!=null){
            this.returnOrNot = 1;
        }

        PLCMethodDeclSymbol methodDeclSymbol = (PLCMethodDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);
        String className = getClassName(ctx);
        packageMethodIOVarSentences(methodDeclSymbol.getVariableMap(), className);

        // Flat 模式：方法变为普通函数，第一个参数为类实例指针
        String returnType;
        if(returnOrNot == 1){
            returnType = methodDeclSymbol.getRuntimeTypeName() != null ? mapToNativeType(methodDeclSymbol.getRuntimeTypeName()) : "auto";
        }else{
            returnType = "void";
        }
        sb.append("\n" + returnType + " " + className + "_" + methodDeclSymbol.getName() + "(" + className + "* this");
        if(!this.funcParaSentences.isEmpty()){
            for(int t = 0; t<this.funcParaSentences.size(); t++){
                sb.append(", "+this.funcParaSentences.get(t));
            }
        }
        sb.append(") {");

        // 传递输入参数值
        for (String funcCallInitSentence : this.funcCallInitSentences) {
            sb.append(funcCallInitSentence);
        }

        // 翻译函数执行内容
        String funcBodyResult = translatorNew.visit(ctx.func_body());
        sb.append(funcBodyResult);

        // 将输出参数值赋值返回
        for (String funcCallOutputSentence : this.funcCallOutputSentences) {
            sb.append(funcCallOutputSentence);
        }
        sb.append("\n}");
        return sb.toString();
    }

    void packageMethodIOVarSentences(Map<String, PLCVariable> valueMap, String className){
        for (PLCVariable value : valueMap.values()) {
            String nativeType = mapToNativeType(value.getRuntimeTypeName());
            if(value.getVarSections() == PLCModifierEnum.VarSections.VAR_INPUT){
                this.callFuncParaSentences.add(value.getName());
                this.funcParaSentences.add(nativeType + " " + value.getName());
            }else if(value.getVarSections() == PLCModifierEnum.VarSections.VAR_OUTPUT){
                this.callFuncParaSentences.add(value.getName());
                this.funcParaSentences.add(nativeType + "& " + value.getName());
                this.funcCallOutputSentences.add("\n\t" + value.getName() + " = " + value.getName() + ";");
            }else if(value.getVarSections() == PLCModifierEnum.VarSections.VAR_IN_OUT){
                this.callFuncParaSentences.add(value.getName());
                this.funcParaSentences.add(nativeType + "& " + value.getName());
                this.funcCallInitSentences.add("\n\t" + value.getName() + " = " + value.getName() + ";");
                this.funcCallOutputSentences.add("\n\t" + value.getName() + " = " + value.getName() + ";");
            }
        }
    }

    /**
     * 从方法声明上下文中推断所属类名
     */
    private String getClassName(PLCSTPARSERParser.Method_declContext ctx) {
        // 尝试从父节点获取类声明上下文
        if (ctx.getParent() != null && ctx.getParent() instanceof PLCSTPARSERParser.Class_declContext) {
            PLCSTPARSERParser.Class_declContext classCtx = (PLCSTPARSERParser.Class_declContext) ctx.getParent();
            return classCtx.class_type_name().identifier().getText();
        }
        // 如果无法推断，返回默认名称
        return "UnknownClass";
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
