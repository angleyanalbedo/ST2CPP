package PLCTranslator;

import antlr4.PLCSTPARSERParser;

public interface TranslatorInterface {
    String translateNode(PLCSTPARSERParser ctx, PLCTranslatorNew translatorNew);
}
