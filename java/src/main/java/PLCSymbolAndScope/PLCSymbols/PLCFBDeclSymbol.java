package PLCSymbolAndScope.PLCSymbols;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

public class PLCFBDeclSymbol extends PLCBaseClassDeclSymbol {

    //返回值声明标识符，根据先后顺序排队
    public ArrayList<Integer> returnVars = new ArrayList<>();

    //入参类型id，根据先后顺序排队
    public ArrayList<Integer> inVars = new ArrayList<>();

    //出入参符号，根据先后顺序排队
    public ArrayList<Integer> inOutVars = new ArrayList<>();


    public void addReturnVar(int returnVarId){
        this.returnVars.add(returnVarId);
    }

    public void addInVar(int inVarId){
        this.inVars.add(inVarId);
    }

    public void addInOutVar(int inOutVarId){
        this.inOutVars.add(inOutVarId);
    }

    public PLCFBDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.FB_DECL;
        varSort = PLCModifierEnum.Sort.FB;
    }
    public PLCFBDeclSymbol(String name, int rowNum){
        super(name, rowNum);
        sort = PLCModifierEnum.Sort.FB_DECL;
        varSort = PLCModifierEnum.Sort.FB;
    }

    public PLCFBDeclSymbol(PLCFBDeclSymbol resource){
        super(resource);
    }


    @Override
    public String toString() {
        return "PLCFBDeclSymbol{" +
                "returnVars=" + returnVars +
                ", inVars=" + inVars +
                ", inOutVars=" + inOutVars +
                ", interfaces=" + interfaces +
                ", namespaces=" + namespaces +
                ", baseClass=" + baseClass.getName() +
                ", classModifier=" + classModifier +
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
        JsonArray jsonArray = new JsonArray();
        for (Integer integer : returnVars) jsonArray.add(integer);
        jsonObject.add("returnVars", jsonArray);
        jsonArray.remove(jsonArray);
        for (Integer integer : inVars) jsonArray.add(integer);
        jsonObject.add("inVars", jsonArray);
        jsonArray.remove(jsonArray);
        for (Integer integer : inOutVars) jsonArray.add(integer);
        jsonObject.add("inOutVars", jsonArray);
        jsonArray.remove(jsonArray);
        for (PLCInterfaceDeclSymbol plcInterfaceDeclSymbol : interfaces) jsonArray.add(plcInterfaceDeclSymbol.toStringJson());
        jsonObject.add("interfaces", jsonArray);
        jsonArray.remove(jsonArray);
        for (PLCNamespaceDeclSymbol plcNamespaceDeclSymbol : namespaces) jsonArray.add(plcNamespaceDeclSymbol.toStringJson());
        jsonObject.add("namespaces", jsonArray);
        jsonObject.addProperty("baseClass", baseClass.getName());
        jsonObject.addProperty("classModifier", classModifier!=null?classModifier.name():null);
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
        jsonSymbol.add("PLCImportScopeTypeDeclType",jsonObject);
        return jsonSymbol;
    }
}
