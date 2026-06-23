package PLCSymbolAndScope.PLCSymbols;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PLCSubrangeDeclSymbol extends PLCTypeDeclSymbol{
    public String getUpperLimit() {
        return upperLimit;
    }

    public void setUpperLimit(String upperLimit) {
        this.upperLimit = upperLimit;
    }

    public String getLowerLimit() {
        return lowerLimit;
    }

    public void setLowerLimit(String lowerLimit) {
        this.lowerLimit = lowerLimit;
    }

    public int getSubTypeId() {
        return subtypeId;
    }

    public void setSubTypeId(int typeId) {
        this.subtypeId = typeId;
        PLCTypeDeclSymbol parentType = PLCTotalSymbolTable.getTypeByTypeID(typeId);
        this.setAssignableSet(parentType.getAssignableSet());
        this.setCalculableSet(parentType.getCalculableSet());
        this.setComparableSet(parentType.getComparableSet());
        this.setEqualitySet(parentType.getEqualitySet());
    }

    //上下界
    private String upperLimit;
    private String lowerLimit;
    private int subtypeId;
    public PLCSubrangeDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.SUBRANGE_DECL;
        varSort = PLCModifierEnum.Sort.SUBRANGE;
    }

    public PLCSubrangeDeclSymbol(String name, int rowNum) {
        super(name, rowNum);
        sort = PLCModifierEnum.Sort.SUBRANGE_DECL;
        varSort = PLCModifierEnum.Sort.SUBRANGE;
    }

    public PLCSubrangeDeclSymbol(PLCSubrangeDeclSymbol resource){
        super(resource);
    }

    @Override
    public String toString() {
        return "PLCSubrangeDeclSymbol{" +
                "upperLimit='" + upperLimit + '\'' +
                ", lowerLimit='" + lowerLimit + '\'' +
                ", subtypeId=" + subtypeId +
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
        jsonObject.addProperty("upperLimit", upperLimit);
        jsonObject.addProperty("lowerLimit", lowerLimit);
        jsonObject.addProperty("subtypeId", subtypeId);
        jsonObject.addProperty("initVar", initVar);
        String varSortName;
        if (varSort != null) varSortName = varSort.name();
        else varSortName = "null";
        jsonObject.addProperty("varSort", varSortName);
        jsonObject.addProperty("symbolId", symbolId);
        jsonObject.addProperty("typeId", typeId);
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("rowNum", rowNum);
        jsonObject.addProperty("columnNum", columnNum);
        String sortName;
        if (sort != null) sortName = sort.name();
        else sortName = "null";
        jsonObject.addProperty("sort", sortName);
        jsonObject.addProperty("runtimeName", runtimeName);
        jsonObject.addProperty("runtimeTypeName", runtimeTypeName);
        JsonObject jsonSymbol = new JsonObject();
        jsonSymbol.add("PLCSubrangeDeclSymbol",jsonObject);
        return jsonSymbol;
    }


}
