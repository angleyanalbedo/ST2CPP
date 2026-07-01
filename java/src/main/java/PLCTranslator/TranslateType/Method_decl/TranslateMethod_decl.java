package PLCTranslator.TranslateType.Method_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.GvlContext;
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
        packageMethodIOVarSentences(methodDeclSymbol.getVariableMap(), className, translatorNew.gvlCtx);

        String returnType;
        if(returnOrNot == 1){
            returnType = methodDeclSymbol.getRuntimeTypeName() != null ? translatorNew.gvlCtx.toNativeType(methodDeclSymbol.getRuntimeTypeName()) : "auto";
        }else{
            returnType = "void";
        }

        if (translatorNew.inClassDecl) {
            // ── CLASS 成员方法：在 class 内部，用原生 C++ this ──
            sb.append("\n\t").append(returnType).append(" ").append(methodDeclSymbol.getName()).append("(");
            if(!this.funcParaSentences.isEmpty()){
                sb.append(this.funcParaSentences.get(0));
                for(int t = 1; t<this.funcParaSentences.size(); t++){
                    sb.append(", ").append(this.funcParaSentences.get(t));
                }
            }
            sb.append(") {");

            for (String s : this.funcCallInitSentences) sb.append(s);
            sb.append(translatorNew.visit(ctx.func_body()));
            for (String s : this.funcCallOutputSentences) sb.append(s);
            sb.append("\n\t}");
        } else {
            // ── FB 方法（遗留）：自由函数 + self 指针 ──
            sb.append("\n").append(returnType).append(" ").append(className).append("_").append(methodDeclSymbol.getName())
              .append("(").append(className).append("* self");
            if(!this.funcParaSentences.isEmpty()){
                for(int t = 0; t<this.funcParaSentences.size(); t++){
                    sb.append(", ").append(this.funcParaSentences.get(t));
                }
            }
            sb.append(") {");

            for (String s : this.funcCallInitSentences) sb.append(s);
            sb.append(translatorNew.visit(ctx.func_body()));
            for (String s : this.funcCallOutputSentences) sb.append(s);
            sb.append("\n}");
        }

        return sb.toString();
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
                this.funcCallOutputSentences.add("\n\t\t" + value.getName() + " = " + value.getName() + ";");
            }else if(value.getVarSections() == PLCModifierEnum.VarSections.VAR_IN_OUT){
                this.callFuncParaSentences.add(value.getName());
                this.funcParaSentences.add(nativeType + "& " + value.getName());
                this.funcCallInitSentences.add("\n\t\t" + value.getName() + " = " + value.getName() + ";");
                this.funcCallOutputSentences.add("\n\t\t" + value.getName() + " = " + value.getName() + ";");
            }
        }
    }

    private String getClassName(PLCSTPARSERParser.Method_declContext ctx) {
        if (ctx.getParent() != null && ctx.getParent() instanceof PLCSTPARSERParser.Class_declContext) {
            PLCSTPARSERParser.Class_declContext classCtx = (PLCSTPARSERParser.Class_declContext) ctx.getParent();
            return classCtx.class_type_name().identifier().getText();
        }
        if (ctx.getParent() != null && ctx.getParent() instanceof PLCSTPARSERParser.Fb_declContext) {
            PLCSTPARSERParser.Fb_declContext fbCtx = (PLCSTPARSERParser.Fb_declContext) ctx.getParent();
            return fbCtx.derived_fb_name().identifier().getText();
        }
        return "UnknownClass";
    }

}
