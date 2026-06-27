package PLCTranslator.TranslateType.Func_decl;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTranslator.PLCTranslatorNew.properties;

public class TranslateFunc_decl {
    //函数调用接口参数
    ArrayList<String> funcParaSentences = new ArrayList<>();

    //函数调用接口内变量初始化
    ArrayList<String> funcCallInitSentences = new ArrayList<>();

    //函数调用接口内返回输出语句
    ArrayList<String> funcCallOutputSentences = new ArrayList<>();

    //函数调用参数语句
    ArrayList<String> callFuncParaSentences = new ArrayList<>();

    public String translateNode(PLCSTPARSERParser.Func_declContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();
        //获取函数符号
        PLCFCDeclSymbol funcSymbol = (PLCFCDeclSymbol) properties.get(ctx).get(0);

        //获取变量段变量信息组装所需语句
        if(ctx.io_var_decls().size() != 0) {
            this.packageIOSentences((ArrayList<PLCSTPARSERParser.Io_var_declsContext>) ctx.io_var_decls(), translatorNew);
        }
        if(ctx.func_var_decls().size() != 0) {
            this.packageFuncVarSentences((ArrayList<PLCSTPARSERParser.Func_var_declsContext>)  ctx.func_var_decls(), translatorNew);
        }
        if(ctx.temp_var_decls().size() != 0) {
            this.packageTempVarSentences((ArrayList<PLCSTPARSERParser.Temp_var_declsContext>) ctx.temp_var_decls(), translatorNew);
        }

        //翻译函数声明
        String funcName = ctx.derived_func_name().getText();
        String returnValueType;
        if(funcSymbol.getReturnTypeId() == -1){
            returnValueType = "void";
        }else{
            returnValueType = PLCTotalSymbolTable.getTypeByTypeID(funcSymbol.getReturnTypeId()).getName();
        }

        // Flat 模式：生成原生 C++ 函数
        StringBuilder params = new StringBuilder();
        if (this.funcParaSentences.size() > 0) {
            params.append(this.funcParaSentences.get(0));
            for (int t = 1; t < this.funcParaSentences.size(); t++) {
                params.append(", ").append(this.funcParaSentences.get(t));
            }
        }

        // 检查 func_body 是否包含实际语句（ANTLR 可能匹配空 stmt_list）
        boolean hasBody = false;
        if (ctx.func_body() != null) {
            PLCSTPARSERParser.Func_bodyContext body = ctx.func_body();
            hasBody = (body.stmt_list() != null && body.stmt_list().stmt().size() > 0)
                    || body.ladder_diagram() != null || body.fb_diagram() != null
                    || body.instruction_list() != null || body.Other_Languages() != null;
        }

        if (hasBody) {
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
            // 外部函数声明（无 body）→ 仅生成原型
            String nativeReturn = mapToNativeType(returnValueType, translatorNew);
            sb.append("\n" + nativeReturn + " " + funcName + "(" + params.toString() + ");\n");
        }

        return sb.toString();
    }

    /**
     * @describe 组装和输入输出变量相关的语句
     * @param ioList io变量段结点
     */
    void packageIOSentences(ArrayList<PLCSTPARSERParser.Io_var_declsContext> ioList, PLCTranslatorNew translatorNew) {
        for (PLCSTPARSERParser.Io_var_declsContext io_var_declsContext : ioList) {
            ArrayList<PLCSymbol> ioVarList = properties.get(io_var_declsContext);
            for (PLCSymbol symbol : ioVarList) {
                PLCVariable tempSymbol = (PLCVariable) symbol;
                PLCModifierEnum.VarSections sections = tempSymbol.getVarSections();

                // 参数使用原生 C++ 类型
                String nativeType = tempSymbol.getRuntimeTypeName();
                nativeType = mapToNativeType(nativeType, translatorNew);
                if(sections == PLCModifierEnum.VarSections.VAR_INPUT){
                    this.funcParaSentences.add(nativeType + " " + tempSymbol.getName());
                    this.callFuncParaSentences.add(tempSymbol.getName());
                }else if(sections == PLCModifierEnum.VarSections.VAR_IN_OUT || sections == PLCModifierEnum.VarSections.VAR_OUTPUT){
                    this.funcParaSentences.add(nativeType + "& " + tempSymbol.getName());
                    this.callFuncParaSentences.add(tempSymbol.getName());
                    this.funcCallOutputSentences.add("\n\t\t" + tempSymbol.getName() + " = " + tempSymbol.getName() + ";");
                }
            }
        }
    }

    void packageFuncVarSentences(ArrayList<PLCSTPARSERParser.Func_var_declsContext> funcVarList, PLCTranslatorNew translatorNew){
        for (PLCSTPARSERParser.Func_var_declsContext func_var_declsContext : funcVarList) {
            ArrayList<PLCSymbol> ioVarList = properties.get(func_var_declsContext);
            for (PLCSymbol symbol : ioVarList) {
                PLCVariable tempSymbol = (PLCVariable) symbol;
                String nativeType = mapToNativeType(tempSymbol.getRuntimeTypeName(), translatorNew);
                this.funcCallInitSentences.add("\n\t" + nativeType + " " + tempSymbol.getName() + " = " + PLCTranslatorNew.codeGen.translateExpr(tempSymbol.getAssignVar()) + ";");
            }
        }
    }

    void packageTempVarSentences(ArrayList<PLCSTPARSERParser.Temp_var_declsContext> tempVarList, PLCTranslatorNew translatorNew){
        for (PLCSTPARSERParser.Temp_var_declsContext temp_var_declsContext : tempVarList) {
            ArrayList<PLCSymbol> ioVarList = properties.get(temp_var_declsContext);
            for (PLCSymbol symbol : ioVarList) {
                PLCVariable tempSymbol = (PLCVariable) symbol;
                String nativeType = mapToNativeType(tempSymbol.getRuntimeTypeName(), translatorNew);
                this.funcCallInitSentences.add("\n\t" + nativeType + " " + tempSymbol.getName() + " = " + PLCTranslatorNew.codeGen.translateExpr(tempSymbol.getAssignVar()) + ";");
            }
        }
    }

    /**
     * 将运行时类型名映射为原生 C++ 类型名
     * 先委托 codeGen.toNativeType（处理 PLC_Struct_Value<ID> 等映射），
     * 再用 switch 映射 PLC_*_Value 包装类型名
     */
    static String mapToNativeType(String runtimeTypeName, PLCTranslatorNew translatorNew) {
        if (runtimeTypeName == null) return "int";
        // 优先使用 codeGen 的类型映射（struct、enum 等）
        String codeGenMapped = translatorNew.codeGen.toNativeType(runtimeTypeName);
        if (codeGenMapped != null && !codeGenMapped.equals(runtimeTypeName)) {
            return codeGenMapped;
        }
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
