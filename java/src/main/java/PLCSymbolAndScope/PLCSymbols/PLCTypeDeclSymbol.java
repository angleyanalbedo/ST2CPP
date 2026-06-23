package PLCSymbolAndScope.PLCSymbols;

import PLCSymbolAndScope.PLCScopeStack;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;

public class PLCTypeDeclSymbol extends PLCSymbol{

    public PLCTypeDeclSymbol() {
        super();
    }

    public PLCTypeDeclSymbol(String name, int rowNum) {
        super(name, rowNum);
    }

    public PLCTypeDeclSymbol(PLCTypeDeclSymbol resource){
        super();
        this.sort = resource.sort;
        this.varSort = resource.varSort;

        this.typeId = resource.typeId;
        this.assignableSet.add(typeId);

        this.symbolId = resource.symbolId;
        this.initVar = resource.initVar;
        this.name = resource.name;
        this.runtimeName = resource.runtimeName;
    }

    //内置类型初始化专供,其他情况不应当调用
    public PLCTypeDeclSymbol(int symbolId, int typeId, String name){
        this.symbolId = symbolId;
        this.typeId = typeId;
        this.name = name;
        this.rowNum = -1;
        this.localScope = PLCScopeStack.globalScope;
        this.localSymbolTable = PLCScopeStack.globalSymbolTable;
    }

    @Override
    public void setTypeId(int typeId) {
        super.setTypeId(typeId);
        this.assignableSet.add(typeId);
    }

    //类型的初始值
    protected String initVar = "";

    public void setInitVar(String initVar) {
        this.initVar = initVar;
    }

    public String getInitVar() {
        return initVar;
    }


    //类型对应的变量的分类,默认确定
    protected PLCModifierEnum.Sort varSort;

    public PLCModifierEnum.Sort getVarSort(){
        return varSort;
    }

    public void setVarSort(PLCModifierEnum.Sort varSort) {
        this.varSort = varSort;
    }


    public HashSet<Integer> getCalculableSet() {
        return calculableSet;
    }

    public HashSet<Integer> getComparableSet() {
        return comparableSet;
    }

    public HashSet<Integer> getEqualitySet() {
        return equalitySet;
    }

    public HashSet<Integer> getAssignableSet() {
        return assignableSet;
    }


    public void setCalculableSet(HashSet<Integer> calculableSet) {
        this.calculableSet = calculableSet;
    }

    public void setComparableSet(HashSet<Integer> comparableSet) {
        this.comparableSet = comparableSet;
    }

    public void setEqualitySet(HashSet<Integer> equalitySet) {
        this.equalitySet = equalitySet;
    }

    public void setAssignableSet(HashSet<Integer> assignableSet) {
        this.assignableSet = assignableSet;
    }

    //存储可进行数学运算的类型
    private HashSet<Integer> calculableSet = new HashSet<>();

    //检查是否可进行数字运算(+-*/)
    public boolean checkCanMathCalcWith(int typeId){
        return this.calculableSet.contains(typeId);
    }
    //添加可进行数学运算的类型id
    public void addCalculableType(int typeId){
        this.calculableSet.add(typeId);
    }

    //存储可进行大小比较的类型id
    private HashSet<Integer> comparableSet = new HashSet<>();

    //检查是否可进行大小比较
    public boolean checkCanCompareWith(int typeId){
        return this.comparableSet.contains(typeId);
    }

    //添加可进行大小比较的类型
    public void addComparableType(int typeId){
        this.comparableSet.add(typeId);
    }

    //存储可进行判等的类型id
    private HashSet<Integer> equalitySet = new HashSet<>();

    //检查是否可进行判等
    public boolean checkCanEqualWith(int typeId){
        return this.equalitySet.contains(typeId);
    }

    //添加可进行判等的类型
    public void addEqualType(int typeId){
        this.equalitySet.add(typeId);
    }

    //存储可进行赋值的类型id
    //thisType := elements of assignableSets
    private HashSet<Integer> assignableSet = new HashSet<>();

    //检查是否可进行赋值
    public boolean checkCanAssignWith(int typeId){
        return this.assignableSet.contains(typeId);
    }

    //添加可进行赋值的类型id
    public void addAssignableType(int typeId){
        this.assignableSet.add(typeId);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("PLCTypeDeclSymbol{" + "initVar='").append(initVar).append('\'').
                append(", varSort=").append(varSort).
                append(", calculableSet=").append(calculableSet).
                append(", comparableSet=").append(comparableSet).
                append(", equalitySet=").append(equalitySet).
                append(", assignableSet=").append(assignableSet).
                append(", symbolId=").append(symbolId).
                append(", typeId=").append(typeId).
                append(", name='").append(name).append('\'').
                append(", rowNum=").append(rowNum).
                append(", columnNum=").append(columnNum).
                append(", sort=").append(sort).
                append(", runtimeName='").append(runtimeName).append('\'').
                append(", runtimeTypeName='").append(runtimeTypeName).append('\'').append('}');
        return new String(str);
    }

    public JsonElement toStringJson() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("initVar", initVar);
        String varSortName;
        if (varSort != null) varSortName = varSort.name();
        else varSortName = "null";
        jsonObject.addProperty("varSort", varSortName);
        JsonArray calculableArray = new JsonArray();
        for(int i : calculableSet) calculableArray.add(i);
        jsonObject.add("calculableSet",calculableArray);
        JsonArray comparableArray = new JsonArray();
        for(int i : calculableSet) comparableArray.add(i);
        jsonObject.add("comparableSet", comparableArray);
        JsonArray equalityArray = new JsonArray();
        for(int i : calculableSet) comparableArray.add(i);
        jsonObject.add("equalitySet", equalityArray);
        JsonArray assignableArray = new JsonArray();
        for(int i : calculableSet) comparableArray.add(i);
        jsonObject.add("assignableSet", assignableArray);
        jsonObject.addProperty("symbolId", symbolId);
        jsonObject.addProperty("typeId", typeId);
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("rowNum", rowNum);
        jsonObject.addProperty("columnNum", columnNum);
        jsonObject.addProperty("sort", sort!=null?sort.name():null);
        jsonObject.addProperty("runtimeName", runtimeName);
        jsonObject.addProperty("runtimeTypeName", runtimeTypeName);
        JsonObject jsonSymbol = new JsonObject();
        jsonSymbol.add("PLCTypeDeclSymbol",jsonObject);
        return jsonSymbol;
    }
}
