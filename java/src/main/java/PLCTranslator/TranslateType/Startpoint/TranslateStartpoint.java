package PLCTranslator.TranslateType.Startpoint;

import PLCSymbolAndScope.PLCSymbols.PLCInterfaceDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

//PLCSt翻译起点
public class TranslateStartpoint {
    public static ArrayList<String> funcInitSentences = new ArrayList<>();

    public String translateNode(PLCSTPARSERParser.StartpointContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        // 使用 CodeGenerator 生成头文件
        if (translatorNew.shouldEmitHeader()) {
            sb.append(PLCTranslatorNew.codeGen.emitHeader());
        }
        // 手动遍历子节点并拼接
        for (int i = 0; i < ctx.getChildCount(); i++) {
            String result = translatorNew.visit(ctx.getChild(i));
            if (result != null) {
                sb.append(result);
            }
        }
        // 尾部追加 POU 注册表（编译器只需描述「有哪些 PROGRAM」）
        if (translatorNew.shouldEmitPOURegistration() && PLCTranslatorNew.codeGen instanceof FlatCodeGenerator) {
            FlatCodeGenerator flatGen = (FlatCodeGenerator) PLCTranslatorNew.codeGen;
            sb.append(flatGen.emitPOURegistration(flatGen.getFileId(), flatGen.getProgramNames()));
        }
        return sb.toString();
    }
}
