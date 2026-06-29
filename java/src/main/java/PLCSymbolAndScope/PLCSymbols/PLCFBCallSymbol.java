package PLCSymbolAndScope.PLCSymbols;

import java.util.ArrayList;
import java.util.List;

public class PLCFBCallSymbol extends PLCSymbol {

    private String fbInstanceName;
    private int fbTypeId = -1;
    private final List<PLCVariable> inputParams = new ArrayList<>();

    // 数组索引 FB 调用支持：Fbs[I](...)
    private boolean isArrayElement = false;
    private String arrayIndexExpr;   // 索引表达式，如 "I"
    private int elementSize = 0;     // 元素大小（字节）

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

    public boolean isArrayElement() {
        return isArrayElement;
    }

    public void setArrayElement(boolean arrayElement) {
        isArrayElement = arrayElement;
    }

    public String getArrayIndexExpr() {
        return arrayIndexExpr;
    }

    public void setArrayIndexExpr(String arrayIndexExpr) {
        this.arrayIndexExpr = arrayIndexExpr;
    }

    public int getElementSize() {
        return elementSize;
    }

    public void setElementSize(int elementSize) {
        this.elementSize = elementSize;
    }

    public void addInputParam(PLCVariable param) {
        inputParams.add(param);
    }

    public List<PLCVariable> getInputParams() {
        return inputParams;
    }
}
