package PLCTranslator.TranslateType.Namespace_decl;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.tree.ParseTree;

public class TranslateNamespace_elements {
    public String translateNode(PLCSTPARSERParser.Namespace_elementsContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            String result = translatorNew.visit(ctx.getChild(i));
            sb.append(result);
        }
        return sb.toString();
    }
}
