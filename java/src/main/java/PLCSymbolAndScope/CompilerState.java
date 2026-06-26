package PLCSymbolAndScope;

import PLCTranslator.FlatCodeGenerator;

public final class CompilerState {

    private CompilerState() {}

    public static void reset() {
        PLCScopeStack.reset();
        IDGenerator.resetStatic();
        FlatCodeGenerator.resetStatic();
    }
}
