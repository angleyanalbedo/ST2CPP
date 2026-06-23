package PLCSymbolAndScope.PLCSymbols;

import java.util.ArrayList;

public interface UsingNamespace {
    ArrayList<PLCNamespaceDeclSymbol> getNamespaces();

    void addNameSpace(PLCNamespaceDeclSymbol namespaceDeclSymbol);
}
