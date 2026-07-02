package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.Invocation;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.TranslateCallFunc;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class TranslateInvocation2 {
    public String translateNode(PLCSTPARSERParser.Invocation2Context ctx, PLCTranslatorNew translatorNew){
        PLCVariable instanceSymbol = PLCTranslatorNew.getVariable(ctx.invocation2branch(), "invocation2 instance");
        String fbInstanceName = instanceSymbol.getName();
        String fbTypeName = translatorNew.gvlCtx.toNativeType(instanceSymbol.getRuntimeTypeName());

        List<String> paramNames = new ArrayList<>();
        List<String> paramValues = new ArrayList<>();
        for (PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()) {
            PLCVariable paraSymbol = PLCTranslatorNew.getVariable(param_assignContext, "invocation2 parameter");
            paramNames.add(paraSymbol.getName());
            String paramValue;
            if (param_assignContext instanceof PLCSTPARSERParser.InputParamContext ip) {
                paramValue = translatorNew.visit(ip.expression());
            } else {
                paramValue = param_assignContext.getText();
            }
            paramValues.add(paramValue);
        }

        // Invocation2 不支持 => 输出参数，传空 map
        return TranslateCallFunc.emitFBCall(fbInstanceName, fbTypeName, paramNames, paramValues, new LinkedHashMap<>(), translatorNew.gvlCtx, translatorNew.inFB);
    }
}
