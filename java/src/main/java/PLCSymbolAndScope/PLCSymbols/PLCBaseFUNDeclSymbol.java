package PLCSymbolAndScope.PLCSymbols;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class PLCBaseFUNDeclSymbol extends PLCImportScopeTypeDeclType implements UsingNamespace, DeclareVariable{
    public PLCBaseFUNDeclSymbol(PLCBaseFUNDeclSymbol resource){
        super(resource);
    }

    public boolean isIfReturned() {
        return ifReturned;
    }

    public void setIfReturned(boolean ifReturned) {
        this.ifReturned = ifReturned;
    }

    private boolean ifReturned = false;

    //直接返回值类型
    public int returnTypeId = -1;
    //直接返回值分类
    public PLCModifierEnum.Sort returnVarSort;

    //方法内使用的变量表格
    HashMap<String, PLCVariable> variableMap = new HashMap<>();

    public int getReturnTypeId() {
        return returnTypeId;
    }

    public void setReturnTypeId(int returnTypeId) {
        this.returnTypeId = returnTypeId;
    }

    public PLCModifierEnum.Sort getReturnVarSort() {
        return returnVarSort;
    }

    public void setReturnVarSort(PLCModifierEnum.Sort returnVarSort) {
        this.returnVarSort = returnVarSort;
    }

    //函数的出入参数，按照声明顺序存放，用PLCVariable的var section区分是入参、出参、出入参
    protected final ArrayList<PLCVariable> accessVars = new ArrayList<>();

    //类使用的命名空间
    public ArrayList<PLCNamespaceDeclSymbol> namespaces = new ArrayList<>();

    public PLCBaseFUNDeclSymbol(){
        super();
    }
    public PLCBaseFUNDeclSymbol(String name, int rowNum){
        super(name, rowNum);
    }

    public void addAccessVar(PLCVariable var){
        this.accessVars.add(var);
    }

    public PLCVariable getAccessVar(String varName){
        for (PLCVariable accessVar : this.accessVars) {
            if(varName.equals(accessVar.getName())){
                return accessVar;
            }
        }
        return null;
    }

    public ArrayList<PLCVariable> getAccessVars() {
        return accessVars;
    }

    //获得最少参数数量
    public int getLessParamAmount(){
        int count = 0;
        for (PLCVariable accessVar : this.accessVars) {
            if(accessVar.getAssignVar().isEmpty()){
                count ++;
            }
        }
        return count;
    }

    //获得最多参数数量
    public int getMaxParamAmount(){
        return this.accessVars.size();
    }

    //检查两个函数签名是否重复
    public boolean checkOverlap(PLCBaseFUNDeclSymbol another){
        ArrayList<PLCVariable> anotherAccessVars = another.getAccessVars();
        if(this.accessVars.size() != anotherAccessVars.size()){
            return false;
        }
        ArrayList<Integer> thisMethodAccessType = new ArrayList<>();
        for (PLCVariable accessVar : this.accessVars) {
            thisMethodAccessType.add(accessVar.getTypeId());
        }

        for (PLCVariable accessVar : anotherAccessVars) {
            int varTypeId = accessVar.getTypeId();
            if(!thisMethodAccessType.contains(varTypeId)){
                return false;
            }
            thisMethodAccessType.remove((Integer) varTypeId);
        }

        if(thisMethodAccessType.isEmpty()){
            return true;
        }
        return false;
    }

    @Override
    public void addNameSpace(PLCNamespaceDeclSymbol namespaceDeclSymbol){
        this.namespaces.add(namespaceDeclSymbol);
    }

    @Override
    public ArrayList<PLCNamespaceDeclSymbol> getNamespaces() {
        return namespaces;
    }

    public String getStdFunction(){
        StringBuilder funcCall = new StringBuilder();
        funcCall.append("::PLC::RFM->getSymbolByID<").append(this.name).append("*>(").append(this.symbolId).append(")->callFunc");
        return new String(funcCall);
    }

    @Override
    public String toString() {
        return "PLCBaseFUNDeclSymbol{" +
                "returnTypeId=" + returnTypeId +
                ", returnVarSort=" + returnVarSort +
                ", accessVars=" + accessVars +
                ", namespaces=" + namespaces +
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
        String name;
        jsonObject.addProperty("returnTypeId", returnTypeId);
        if (returnVarSort!=null) name=returnVarSort.toString();
        else name="null";
        jsonObject.addProperty("returnVarSort", name);
        JsonArray accessVarArray = new JsonArray();
        for (PLCVariable plcVariable : accessVars ) {
            accessVarArray.add( plcVariable.toStringJson());
        }
        jsonObject.add("accessVars",accessVarArray);
        JsonArray NamespaceDeclArray = new JsonArray();
        for (PLCNamespaceDeclSymbol plcNamespaceDeclSymbol : namespaces ) {
            NamespaceDeclArray.add( plcNamespaceDeclSymbol.toStringJson());
        }
        jsonObject.add("namespaces",NamespaceDeclArray);
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
        jsonSymbol.add("PLCMethodDeclSymbol",jsonObject);
        return jsonSymbol;
    }

    @Override
    public HashMap<String, PLCVariable> getVariableMap() {
        return variableMap;
    }

    @Override
    public PLCVariable getVariable(String name) {
        return variableMap.get(name);
    }

    @Override
    public void addVariable(PLCVariable var) {
        this.variableMap.put(var.getName(), var);
    }

    @Override
    public void addAllVariable(Collection<PLCVariable> vars) {
        for (PLCVariable var : vars) {
            this.addVariable(var);
        }
    }
}
