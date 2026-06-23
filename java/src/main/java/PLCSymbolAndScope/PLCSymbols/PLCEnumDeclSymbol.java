package PLCSymbolAndScope.PLCSymbols;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

//枚举量定义时也会引入一个作用域
public class PLCEnumDeclSymbol extends PLCImportScopeTypeDeclType {

    //枚举常量的类型id
    private int enumConstTypeId;

    public int getEnumConstTypeId() {
        return enumConstTypeId;
    }

    public void setEnumConstTypeId(int enumConstTypeId) {
        this.enumConstTypeId = enumConstTypeId;
    }

    //枚举类型的内部常量
    private ArrayList<PLCVariable> enumValues = new ArrayList<>();

    public ArrayList<PLCVariable> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(ArrayList<PLCVariable> enumValues) {
        this.enumValues = enumValues;
    }

    public void addEnumValue(PLCVariable enumValue){
        this.enumValues.add(enumValue);
    }

    //返回枚举类型内的枚举常量
    public PLCVariable findEnumValue(String name){
        for (PLCVariable enumValue : this.enumValues) {
            if(name.equals(enumValue.getName())){
                return enumValue;
            }
        }
        return null;
    }

    //默认的枚举常量
    private PLCVariable initEnumVar;

    public PLCVariable getInitEnumVar() {
        return initEnumVar;
    }

    public void setInitEnumVar(PLCVariable initEnumVar) {
        this.initEnumVar = initEnumVar;
    }






    public ArrayList<String> getEnumElemName() {
        return enumElemName;
    }

    public void setEnumElemName(ArrayList<String> enumElemName) {
        this.enumElemName = enumElemName;
    }

    public void addEnumElemName(String name){
        this.enumElemName.add(name);
    }


    //枚举常量列表
    public ArrayList<String> enumElemName = new ArrayList<>();

    //枚举常量的sort
    public PLCModifierEnum.Sort enumConstSort;

    public PLCModifierEnum.Sort getEnumConstSort() {
        return enumConstSort;
    }

    public PLCEnumDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.ENUM_DECL;
        varSort = PLCModifierEnum.Sort.ENUM;
    }
    public PLCEnumDeclSymbol(String name, int rowNum){
        super(name, rowNum);
        sort = PLCModifierEnum.Sort.ENUM_DECL;
        varSort = PLCModifierEnum.Sort.ENUM;
    }

    public PLCEnumDeclSymbol(PLCEnumDeclSymbol resource){
        super(resource);
    }

    @Override
    public void setTypeId(int typeId) {
        super.setTypeId(typeId);
        this.setRuntimeName("PLC_Enum_Value<" + typeId + ">");
    }

    @Override
    public String toString() {
        return "PLCEnumDeclSymbol{" +
                "enumConstTypeId=" + enumConstTypeId +
                ", enumValues=" + enumValues +
                ", initEnumVar=" + initEnumVar +
                ", enumElemName=" + enumElemName +
                ", enumConstSort=" + enumConstSort +
                ", initVar='" + initVar + '\'' +
                ", varSort=" + varSort +
                ", symbolId=" + symbolId +
                ", typeId=" + typeId +
                ", name='" + name + '\'' +
                ", rowNum=" + rowNum +
                ", columnNum=" + columnNum +
                ", sort=" + sort +
                ", runtimeName='" + runtimeName + '\'' +
                ", runtimeTypeName='" + runtimeTypeName + '\'' +
                '}';
    }

    public JsonElement toStringJson() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("enumConstTypeId", enumConstTypeId);
        JsonArray jsonArray = new JsonArray();
        for(PLCVariable plcVariable : enumValues){
            jsonArray.add(plcVariable.toStringJson());
        }
        jsonObject.add("enumValues", jsonArray);
        jsonArray.remove(jsonArray);
        jsonObject.add("initEnumVar", initEnumVar!=null?initEnumVar.toStringJson():null);
        jsonArray.remove(jsonArray);
        for(String  enumName : enumElemName){
            jsonArray.add(enumName);
        }
        jsonObject.add("enumElemName", jsonArray);
        jsonObject.addProperty("enumConstSort", enumConstSort!=null?enumConstSort.name():null);
        jsonObject.addProperty("initVar", initVar);
        jsonObject.addProperty("varSort", varSort!=null?varSort.name():null);
        jsonObject.addProperty("symbolId", symbolId);
        jsonObject.addProperty("typeId", typeId);
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("rowNum", rowNum);
        jsonObject.addProperty("columnNum", columnNum);
        jsonObject.addProperty("sort", sort!=null?sort.name():null);
        jsonObject.addProperty("runtimeName", runtimeName);
        jsonObject.addProperty("runtimeTypeName", runtimeTypeName);
        JsonObject jsonSymbol = new JsonObject();
        jsonSymbol.add("PLCEnumDeclSymbol",jsonObject);
        return jsonSymbol;
    }
}
