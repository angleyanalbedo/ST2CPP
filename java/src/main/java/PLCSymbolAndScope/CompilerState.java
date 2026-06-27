package PLCSymbolAndScope;

import PLCTranslator.GvlContext;

public final class CompilerState {

    private CompilerState() {}

    public static void reset() {
        PLCScopeStack.reset();
        IDGenerator.resetStatic();
        GvlContext.resetStatic();
    }
}
