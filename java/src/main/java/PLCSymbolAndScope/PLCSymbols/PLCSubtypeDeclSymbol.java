package PLCSymbolAndScope.PLCSymbols;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PLCSubtypeDeclSymbol extends PLCTypeDeclSymbol{
    public PLCSubtypeDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.SUBTYPE_DECL;
        varSort = PLCModifierEnum.Sort.SUBTYPE;
    }
    public PLCSubtypeDeclSymbol(String name, int rowNum){
        super(name, rowNum);
        sort = PLCModifierEnum.Sort.SUBTYPE_DECL;
        varSort = PLCModifierEnum.Sort.SUBTYPE;
    }

    public PLCSubtypeDeclSymbol(PLCSubtypeDeclSymbol resource){
        super(resource);
        sort = PLCModifierEnum.Sort.SUBTYPE_DECL;
        varSort = PLCModifierEnum.Sort.SUBTYPE;
    }

    public PLCTypeDeclSymbol getParentSymbol() {
        return parentType;
    }

    //设置子类型的父类型
    public void setParentType(PLCTypeDeclSymbol plcTypeDeclSymbol) {
        this.parentType = plcTypeDeclSymbol;
        this.parentType.setAssignableSet(plcTypeDeclSymbol.getAssignableSet());
        this.parentType.setCalculableSet(plcTypeDeclSymbol.getCalculableSet());
        this.parentType.setComparableSet(plcTypeDeclSymbol.getComparableSet());
        this.parentType.setEqualitySet(plcTypeDeclSymbol.getEqualitySet());
    }

    //此类型的子类型
    private PLCTypeDeclSymbol parentType;

    @Override
    public String toString() {
        return "PLCSubtypeDeclSymbol{" +
                "parentType=" + parentType +
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

        jsonObject.add("parentType", parentType.toStringJson());
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
        jsonSymbol.add("PLCSubtypeDeclSymbol",jsonObject);
        return jsonSymbol;
    }
}
//TYPE声明内的变量，类型define

