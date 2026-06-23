package PLCSymbolAndScope.PLCSymbols;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public interface DeclareVariable {
    HashMap<String, PLCVariable> getVariableMap();

    PLCVariable getVariable(String name);

    void addVariable(PLCVariable var);

    void addAllVariable(Collection<PLCVariable> vars);
}
