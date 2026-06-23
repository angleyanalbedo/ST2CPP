package PLCTranslator.TranslateType.Namespace_decl;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateNamespace_elements {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Namespace_elementsContext ctx, PLCTranslatorNew translatorNew){
        translatorNew.visitChildren(ctx);//访问所有子节点
        return null;
    }
}
