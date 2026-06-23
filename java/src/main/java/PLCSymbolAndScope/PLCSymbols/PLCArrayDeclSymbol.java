package PLCSymbolAndScope.PLCSymbols;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PLCArrayDeclSymbol extends PLCTypeDeclSymbol{

    //内部数据类型
    private int elementTypeId;

    public int getElementTypeId() {
        return elementTypeId;
    }

    public void setElementTypeId(int elementTypeId) {
        this.elementTypeId = elementTypeId;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    //维度数
    private int dimension;

    public PLCArrayDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.ARRAY_DECL;
        varSort = PLCModifierEnum.Sort.ARRAY;
    }
    public PLCArrayDeclSymbol(String name, int rowNum){
        super(name, rowNum);
        sort = PLCModifierEnum.Sort.ARRAY_DECL;
        varSort = PLCModifierEnum.Sort.ARRAY;
    }

    public PLCArrayDeclSymbol(PLCArrayDeclSymbol resource){
        super(resource);
        this.dimension = resource.dimension;
        this.elementTypeId = resource.typeId;
    }

    @Override
    public void setTypeId(int typeId) {
        super.setTypeId(typeId);
        this.setRuntimeName("PLC_Array_Value<" + typeId + ">");
    }

    @Override
    public String toString() {
        return "PLCArrayDeclSymbol{" +
                "elementTypeId=" + elementTypeId +
                ", dimension=" + dimension +
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

        jsonObject.addProperty("elementTypeId", elementTypeId);
        jsonObject.addProperty("dimension", dimension);
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
        jsonSymbol.add("PLCArrayDeclSymbol",jsonObject);
        return jsonSymbol;
    }
}
