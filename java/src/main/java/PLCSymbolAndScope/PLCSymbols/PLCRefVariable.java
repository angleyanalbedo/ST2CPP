package PLCSymbolAndScope.PLCSymbols;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PLCRefVariable extends PLCVariable{
    private PLCVariable referredVariable;

    public int getReferredLevel() {
        return referredLevel;
    }

    public void setReferredLevel(int referredLevel) {
        this.referredLevel = referredLevel;
    }

    private int referredLevel = 0;

    public PLCRefVariable(){
        super();
        this.sort = PLCModifierEnum.Sort.REF;
    }

    public PLCVariable getReferredVariable() {
        return referredVariable;
    }

    public void setReferredVariable(PLCVariable referredVariable) {
        this.referredVariable = referredVariable;
    }

    public JsonElement toStringJson() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("varSections", varSections.name());
        jsonObject.addProperty("accessModifier", accessModifier.name());
        jsonObject.addProperty("retainQualifiers", retainQualifiers.name());
        jsonObject.addProperty("ifConst", ifConst);
        jsonObject.addProperty("symbolId", symbolId);
        jsonObject.addProperty("typeId", typeId);
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("rowNum", rowNum);
        jsonObject.addProperty("columnNum", columnNum);
        jsonObject.addProperty("sort", sort!=null?sort.name():null);
        jsonObject.addProperty("runtimeName", runtimeName);
        jsonObject.addProperty("runtimeTypeName", runtimeTypeName);
        jsonObject.addProperty("referredLevel", referredLevel);
        jsonObject.addProperty("referredVariable",referredVariable!=null?referredVariable.getName():null);
        JsonObject jsonSymbol = new JsonObject();
        jsonSymbol.add("PLCVariable",jsonObject);
        return jsonSymbol;
    }
}
