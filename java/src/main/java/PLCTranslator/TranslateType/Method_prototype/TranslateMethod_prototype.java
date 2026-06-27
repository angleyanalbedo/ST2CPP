package PLCTranslator.TranslateType.Method_prototype;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateMethod_prototype {

    public String translateNode(PLCSTPARSERParser.Method_prototypeContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        String methodName = ctx.method_name().getText();
        String returnType = "void";
        if(ctx.data_type_access()!=null) {
            PLCTypeDeclSymbol retType = (PLCTypeDeclSymbol) PLCTranslatorNew.properties.get(ctx.data_type_access()).get(0);
            returnType = translatorNew.gvlCtx.toNativeType(retType.getRuntimeTypeName());
        }

        ArrayList<String> params = new ArrayList<>();
        for (PLCSTPARSERParser.Io_var_declsContext io_var_decl : ctx.io_var_decls()) {
            ArrayList<PLCSymbol> ioList = PLCTranslatorNew.properties.get(io_var_decl);
            for (PLCSymbol symbol : ioList) {
                PLCVariable var = (PLCVariable) symbol;
                String nativeType = translatorNew.gvlCtx.toNativeType(var.getRuntimeTypeName());
                params.add(nativeType + " " + var.getName());
            }
        }

        sb.append("\n\tvirtual ").append(returnType).append(" ").append(methodName).append("(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(params.get(i));
        }
        sb.append(") = 0;\n");
        return sb.toString();
    }
}
