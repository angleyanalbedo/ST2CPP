package PLCTranslator.TranslateType.Prog_decl;

import PLCSymbolAndScope.PLCSymbols.PLCArrayDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCEnumDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.packageFactory;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;
import static PLCTranslator.TranslateType.Startpoint.TranslateStartpoint.funcInitSentences;

public class TranslateProg_decl {

    packageFactory pFactory = new packageFactory();

    public ArrayList<String> translateNode(PLCSTPARSERParser.Prog_declContext ctx, PLCTranslatorNew translatorNew) {
        //********************************************全局静态函数初始化***************************************************
        writeTarget("\nvoid initFunc(){");

        for(String funcInitSentence : funcInitSentences){
            writeTarget("\n\t"+funcInitSentence);
        }
        writeTarget("\n}\n");


        //*********************************************main函数翻译******************************************************

        writeTarget("\n"+"int main(){");
        //翻译静态方法
        writeTarget("\n\tinitFunc();");
        //翻译变量段
        for (PLCSTPARSERParser.Func_var_declsContext func_var_decl : ctx.func_var_decls()) {
            ArrayList<PLCSymbol> funcVarDecl = PLCTranslatorNew.properties.get(func_var_decl);
            //翻译每一条变量声明定义语句
            for (PLCSymbol symbol : funcVarDecl) {
                if(symbol instanceof PLCEnumDeclSymbol enumDeclSymbol){
                } else if (symbol instanceof PLCArrayDeclSymbol varSymbol) {
//                    System.out.println("auto " + symbol.getRuntimeName() +"="+"new "+varSymbol.getRuntimeTypeName()
//                            +"("+varSymbol.getInitVar()+");");
                }else{
                    PLCVariable varSymbol = (PLCVariable) symbol;
                    //组装变量声明
//                    System.out.println(pFactory.packagePROGVarDeclSentences(symbol.getName(), varSymbol.getRuntimeTypeName(),
//                            varSymbol.getAssignVar()));
                    writeTarget("\n\t"+pFactory.packagePROGVarDeclSentences(symbol.getName(), varSymbol.getRuntimeTypeName(),
                            varSymbol.getAssignVar()));
                }
            }

        }

        for (PLCSTPARSERParser.Temp_var_declsContext temp_var_decl : ctx.temp_var_decls()) {
            ArrayList<PLCSymbol> tempVarDecl = PLCTranslatorNew.properties.get(temp_var_decl);

            for (PLCSymbol symbol : tempVarDecl) {
                if(symbol instanceof PLCEnumDeclSymbol enumDeclSymbol){
//                    System.out.println("auto " + symbol.getRuntimeName() +"="+"new "+enumDeclSymbol.getRuntimeTypeName()
//                            +"("+enumDeclSymbol.getEnumValues()+");");

                } else if (symbol instanceof PLCArrayDeclSymbol varSymbol) {
//                    System.out.println("auto " + symbol.getRuntimeName() +"="+"new "+varSymbol.getRuntimeTypeName()
//                            +"("+varSymbol.getInitVar()+");");

                }else{
                    PLCVariable varSymbol = (PLCVariable) symbol;
//                    System.out.println(pFactory.packagePROGVarDeclSentences(symbol.getRuntimeName(), varSymbol.getRuntimeTypeName(),
//                            varSymbol.getAssignVar()));
                    writeTarget("\n\t"+pFactory.packagePROGVarDeclSentences(symbol.getRuntimeName(), varSymbol.getRuntimeTypeName(),
                            varSymbol.getAssignVar()));
                }
            }

        }

        for (PLCSTPARSERParser.Other_var_declsContext other_var_decl : ctx.other_var_decls()) {
            ArrayList<PLCSymbol> otherVarDecl = PLCTranslatorNew.properties.get(other_var_decl);

            for (PLCSymbol symbol : otherVarDecl) {
                if(symbol instanceof PLCEnumDeclSymbol enumDeclSymbol){
//                    System.out.println("auto " + symbol.getRuntimeName() +"="+"new "+enumDeclSymbol.getRuntimeTypeName()
//                            +"("+enumDeclSymbol.getEnumValues()+");");

                } else if (symbol instanceof PLCArrayDeclSymbol varSymbol) {
//                    System.out.println("auto " + symbol.getRuntimeName() +"="+"new "+varSymbol.getRuntimeTypeName()
//                            +"("+varSymbol.getInitVar()+");");

                }else{
                    PLCVariable varSymbol = (PLCVariable) symbol;
//                    System.out.println(pFactory.packagePROGVarDeclSentences(symbol.getRuntimeName(), varSymbol.getRuntimeTypeName(),
//                            varSymbol.getAssignVar()));
                    writeTarget("\n\t"+pFactory.packagePROGVarDeclSentences(symbol.getRuntimeName(), varSymbol.getRuntimeTypeName(),
                            varSymbol.getAssignVar()));
                }
            }
        }

        translatorNew.visit(ctx.fb_body());
//        System.out.println("}");
        writeTarget("\n}");
        return null;
    }
}
