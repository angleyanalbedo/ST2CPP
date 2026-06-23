package PLCSymbolAndScope.PLCSymbols;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class   PLCStructDeclSymbol extends PLCImportScopeTypeDeclType {
    public boolean getIfOverlap() {
        return ifOverlap;
    }

    public void setIfOverlap(boolean ifOverlap) {
        this.ifOverlap = ifOverlap;
    }

    //overlap标签
    public boolean ifOverlap = false;

    public PLCStructDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.STRUCT_DECL;
        varSort = PLCModifierEnum.Sort.STRUCT;
        this.runtimeName = "PLC_Struct_Decl<" + this.typeId + ">";
    }

    public PLCStructDeclSymbol(String name, int rowNum){
        super(name, rowNum);
        sort = PLCModifierEnum.Sort.STRUCT_DECL;
        varSort = PLCModifierEnum.Sort.STRUCT;
    }

    public PLCStructDeclSymbol(PLCStructDeclSymbol resource){
        super(resource);
        sort = PLCModifierEnum.Sort.STRUCT_DECL;
        varSort = PLCModifierEnum.Sort.STRUCT;
    }

    @Deprecated
    public ArrayList<PLCStructDeclSymbol> getStructElements() {
        return structElements;
    }

    @Deprecated
    public void addPlcStructDeclSymbols(PLCStructDeclSymbol plcStructDeclSymbol){
        this.structElements.add(plcStructDeclSymbol);
    }

    @Deprecated
    public void setStructElements(ArrayList<PLCStructDeclSymbol> plcStructDeclSymbolArrayList) {
        this.structElements = plcStructDeclSymbolArrayList;
    }

    private ArrayList<PLCStructDeclSymbol> structElements = new ArrayList<>();

    public boolean isIfConst() {
        return ifConst;
    }

    public void setIfConst(boolean ifConst) {
        this.ifConst = ifConst;
    }

    private boolean ifConst = false;


    @Override
    public void setTypeId(int typeId) {
        super.setTypeId(typeId);
        this.setRuntimeName("PLC_Struct_Value<" + typeId + ">");
    }

    @Override
    public String toString() {
        return "PLCStructDeclSymbol{" +
                "ifOverlap=" + ifOverlap +
                ", plcStructDeclSymbols=" + structElements +
                ", ifConst=" + ifConst +
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

        jsonObject.addProperty("ifOverlap", ifOverlap);
        jsonObject.addProperty("ifConst", ifConst);
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
        JsonArray jsonArray = new JsonArray();
        for (PLCStructDeclSymbol plcStructDeclSymbol : structElements) {
            jsonArray.add( plcStructDeclSymbol.toStringJson());
        }
        jsonObject.add("PLCStructDeclSymbolList",jsonArray);
        JsonObject jsonSymbol = new JsonObject();
        jsonSymbol.add("PLCStructDeclSymbol",jsonObject);
        return jsonSymbol;
    }

    //变量列表
    private final ArrayList<PLCVariable> variables = new ArrayList<>();

    public ArrayList<PLCVariable> getVariables() {
        return variables;
    }


    public void addVariable(PLCVariable var) {
        this.variables.add(var);
    }

    public void addAllVariable(ArrayList<PLCVariable> vars) {
        this.variables.addAll(vars);
    }
}
