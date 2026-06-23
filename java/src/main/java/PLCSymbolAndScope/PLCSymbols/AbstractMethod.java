package PLCSymbolAndScope.PLCSymbols;

import java.util.ArrayList;

public interface AbstractMethod {
    void addAbstractMethod(PLCMethodDeclSymbol method);

    void addAllAbsMethods(ArrayList<PLCMethodDeclSymbol> methods);

    ArrayList<PLCMethodDeclSymbol> getAbstractMethods();
}
