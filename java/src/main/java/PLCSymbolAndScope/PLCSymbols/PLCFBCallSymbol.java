package PLCSymbolAndScope.PLCSymbols;

import java.util.ArrayList;
import java.util.List;

public class PLCFBCallSymbol extends PLCSymbol {

    private String fbInstanceName;
    private int fbTypeId = -1;
    private final List<PLCVariable> inputParams = new ArrayList<>();

    public PLCFBCallSymbol() {
        super();
    }

    public String getFbInstanceName() {
        return fbInstanceName;
    }

    public void setFbInstanceName(String fbInstanceName) {
        this.fbInstanceName = fbInstanceName;
    }

    public int getFbTypeId() {
        return fbTypeId;
    }

    public void setFbTypeId(int fbTypeId) {
        this.fbTypeId = fbTypeId;
    }

    public void addInputParam(PLCVariable param) {
        inputParams.add(param);
    }

    public List<PLCVariable> getInputParams() {
        return inputParams;
    }
}
