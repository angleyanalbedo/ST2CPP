package PLCSymbolAndScope.PLCSymbols;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

public class PLCNamespaceDeclSymbol extends PLCImportScopeTypeDeclType implements UsingNamespace{
    public PLCNamespaceDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.NAMESPACE_DECL;
        varSort = PLCModifierEnum.Sort.NAMESPACE;
    }

    public PLCNamespaceDeclSymbol(String name, int rowNum){
        super(name, rowNum);
        sort = PLCModifierEnum.Sort.NAMESPACE_DECL;
        varSort = PLCModifierEnum.Sort.NAMESPACE;
    }

    public PLCNamespaceDeclSymbol(PLCNamespaceDeclSymbol resource){
        super(resource);
    }

    ArrayList<PLCNamespaceDeclSymbol> usingNamespaceList = new ArrayList<>();

    @Override
    public ArrayList<PLCNamespaceDeclSymbol> getNamespaces() {
        return this.usingNamespaceList;
    }

    @Override
    public void addNameSpace(PLCNamespaceDeclSymbol namespaceDeclSymbol) {
        this.usingNamespaceList.add(namespaceDeclSymbol);
    }

    @Override
    public String toString() {
        return "PLCNamespaceDeclSymbol{" +
                "usingNamespaceList=" + usingNamespaceList +
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
        for (PLCNamespaceDeclSymbol plcNamespaceDeclSymbol : usingNamespaceList ) {
            jsonArray.add( plcNamespaceDeclSymbol.toStringJson());
        }
        jsonObject.add("usingNamespaceList",jsonArray);
        JsonObject jsonSymbol = new JsonObject();
        jsonSymbol.add("PLCNamespaceDeclSymbol",jsonObject);
        return jsonSymbol;
    }
}
