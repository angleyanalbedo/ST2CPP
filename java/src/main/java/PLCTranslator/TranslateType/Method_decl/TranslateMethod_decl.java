package PLCTranslator.TranslateType.Method_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

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
        packageMethodIOVarSentences(methodDeclSymbol.getVariableMap(), className, translatorNew.gvlCtx);

        // 收集类成员变量名（从 CLASS 的 variableMap 获取，排除 INPUT/OUTPUT/IN_OUT 参数）
        Set<String> memberNames = new HashSet<>();
        collectClassMemberNames(ctx, memberNames);

        // Flat 模式：方法变为普通函数，第一个参数为类实例指针
        String returnType;
        if(returnOrNot == 1){
            returnType = methodDeclSymbol.getRuntimeTypeName() != null ? translatorNew.gvlCtx.toNativeType(methodDeclSymbol.getRuntimeTypeName()) : "auto";
        }else{
            returnType = "void";
        }
        sb.append("\n").append(returnType).append(" ").append(className).append("_").append(methodDeclSymbol.getName())
          .append("(").append(className).append("* self");
        if(!this.funcParaSentences.isEmpty()){
            for(int t = 0; t<this.funcParaSentences.size(); t++){
                sb.append(", ").append(this.funcParaSentences.get(t));
            }
        }
        sb.append(") {");

        // 传递输入参数值
        for (String funcCallInitSentence : this.funcCallInitSentences) {
            sb.append(funcCallInitSentence);
        }

        // 翻译函数执行内容
        String funcBodyResult = translatorNew.visit(ctx.func_body());
        // 为成员变量添加 self-> 前缀
        funcBodyResult = addSelfPrefix(funcBodyResult, memberNames);
        sb.append(funcBodyResult);

        // 将输出参数值赋值返回
        for (String funcCallOutputSentence : this.funcCallOutputSentences) {
            sb.append(funcCallOutputSentence);
        }
        sb.append("\n}");
        return sb.toString();
    }

    /**
     * 为成员变量名添加 self-> 前缀。
     * 仅替换独立的标识符（前后非字母数字下划线），避免误替换子串。
     */
    private String addSelfPrefix(String body, Set<String> memberNames) {
        if (body == null || memberNames.isEmpty()) return body;
        for (String name : memberNames) {
            // 匹配独立标识符：(?<![a-zA-Z0-9_])NAME(?![a-zA-Z0-9_])
            body = body.replaceAll("(?<![a-zA-Z0-9_])" + name + "(?![a-zA-Z0-9_])", "self->" + name);
        }
        return body;
    }

    void packageMethodIOVarSentences(Map<String, PLCVariable> valueMap, String className, GvlContext gvlCtx){
        for (PLCVariable value : valueMap.values()) {
            String nativeType = gvlCtx.toNativeType(value.getRuntimeTypeName());
            if(value.getVarSections() == PLCModifierEnum.VarSections.VAR_INPUT){
                this.callFuncParaSentences.add(value.getName());
                this.funcParaSentences.add(nativeType + " " + value.getName());
            }else if(value.getVarSections() == PLCModifierEnum.VarSections.VAR_OUTPUT){
                this.callFuncParaSentences.add(value.getName());
                this.funcParaSentences.add(nativeType + "& " + value.getName());
                this.funcCallOutputSentences.add("\n\t" + value.getName() + " = self->" + value.getName() + ";");
            }else if(value.getVarSections() == PLCModifierEnum.VarSections.VAR_IN_OUT){
                this.callFuncParaSentences.add(value.getName());
                this.funcParaSentences.add(nativeType + "& " + value.getName());
                this.funcCallInitSentences.add("\n\tself->" + value.getName() + " = " + value.getName() + ";");
                this.funcCallOutputSentences.add("\n\t" + value.getName() + " = self->" + value.getName() + ";");
            }
        }
    }

    /**
     * 从父 CLASS 声明中收集成员变量名（排除方法参数）
     */
    private void collectClassMemberNames(PLCSTPARSERParser.Method_declContext ctx, Set<String> memberNames) {
        if (ctx.getParent() == null || !(ctx.getParent() instanceof PLCSTPARSERParser.Class_declContext)) return;
        PLCSTPARSERParser.Class_declContext classCtx = (PLCSTPARSERParser.Class_declContext) ctx.getParent();
        ArrayList<PLCSymbol> classSymbols = PLCTranslatorNew.properties.get(classCtx);
        if (classSymbols == null || classSymbols.isEmpty()) return;
        PLCSymbol classSym = classSymbols.get(0);
        if (!(classSym instanceof PLCClassDeclSymbol)) return;
        PLCClassDeclSymbol classDecl = (PLCClassDeclSymbol) classSym;
        for (PLCVariable v : classDecl.getVariableMap().values()) {
            memberNames.add(v.getName());
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

}
