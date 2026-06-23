package PLCTranslator;

import antlr4.PLCSTPARSERParser;

public interface TranslatorInterface<T> {
    T translateNode(PLCSTPARSERParser ctx, PLCTranslatorNew translatorNew);

}
