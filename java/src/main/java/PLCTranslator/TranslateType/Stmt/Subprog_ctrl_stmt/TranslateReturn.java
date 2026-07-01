package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt;

import antlr4.PLCSTPARSERParser;
import PLCTranslator.PLCTranslatorNew;

public class TranslateReturn {
    public String translateNode(PLCSTPARSERParser.ReturnContext ctx, PLCTranslatorNew translatorNew){
        // 非 void FC：返回值已由赋值语句处理，跳过 bare return;
        if (translatorNew.currentFuncReturnTypeId != -1) {
            return "";
        }
        return "\n\treturn;";
    }
}
