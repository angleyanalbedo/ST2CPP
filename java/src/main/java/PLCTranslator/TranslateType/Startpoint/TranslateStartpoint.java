package PLCTranslator.TranslateType.Startpoint;

import PLCSymbolAndScope.PLCSymbols.PLCInterfaceDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

//PLCSt翻译起点
public class TranslateStartpoint {
    public static ArrayList<String> funcInitSentences = new ArrayList<>();

    public ArrayList<String> translateNode(PLCSTPARSERParser.StartpointContext ctx, PLCTranslatorNew translatorNew){
        // 使用 CodeGenerator 生成头文件（OOP 或 Flat）
        PLCTranslatorNew.codeGen.emitHeader();
        translatorNew.visitChildren(ctx);
        return null;
    }
}
