package PLCSymbolAndScope.PLCSymbols;

import java.util.ArrayList;

public interface DeclareMethod {
    void addMethod(PLCMethodDeclSymbol method);

    void addAllMethods(ArrayList<PLCMethodDeclSymbol> methods);

    ArrayList<PLCMethodDeclSymbol> getMethods();
}
