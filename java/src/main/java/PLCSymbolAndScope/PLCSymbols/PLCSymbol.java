package PLCSymbolAndScope.PLCSymbols;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;

//需要设置符号ID、类型ID、所属作用域引用、所属符号表引用、符号名、列数行数

public class PLCSymbol {
    //每个符号唯一的ID 在构造方法中自动生成
    public int symbolId;

    //符号的类型标识符，内置类型的类型标识符是内置的，自定义类型的标识符由程序给出
    public int typeId;

    //符号名称
    public String name;

    //符号所在行数
    public int rowNum;

    //符号所在列数
    public int columnNum;

    public PLCScope getLocalScope() {
        return localScope;
    }

    public void setLocalScope(PLCScope localScope) {
        this.localScope = localScope;
    }

    //符号所属作用域, 在初始化时自动设置，默认为栈顶作用域，也可以手动设置
    public PLCScope localScope;

    //指向所属符号表,默认为栈顶作用域符号表
    public PLCSymbolTable localSymbolTable;

    //符号的分类,起到辅助区分的作用
    protected PLCModifierEnum.Sort sort;

    //符号在runtime里的名称
    //对类型而言是 类型在runtime里的值类名称，如PLC_Integer_Value<0>,在设置typeid（能确定时）时确定
    //对变量而言是 变量名的实体，即*name，设置名称时确定
    protected String runtimeName;

    //从变量声明的作用域看起，看变量的类型，得到的带命名空间等信息的runtime值类型名称
    protected String runtimeTypeName;

    public String getRuntimeTypeName() {
        return runtimeTypeName;
    }

    public void setRuntimeTypeName(String runtimeTypeName) {
        this.runtimeTypeName = runtimeTypeName;
    }


    //具体参数构造方法
    public PLCSymbol(String name, int rowNum){
        this.name = name;
        this.rowNum = rowNum;
        this.localSymbolTable = PLCScopeStack.currentSymbolTable;
        this.localScope = PLCScopeStack.currentScope;
    }

    //默认构造方法，需要另外设置typeId name rowNum
    public PLCSymbol(){
        this.localSymbolTable = PLCScopeStack.currentSymbolTable;
        this.localScope = PLCScopeStack.currentScope;
    }


    //根据符号表或作用域设置所属
    public void setLocal(PLCScope localScope){
        this.localSymbolTable = localScope.getScopeSymbolTable();
        this.localScope = localScope;
    }
    public void setLocal(PLCSymbolTable localTable){
        this.localSymbolTable = localTable;
        this.localScope = localTable.getTableScope();
    }

    public void setTypeId(int typeId){

        this.typeId = typeId;

    }

    public void setSort(PLCModifierEnum.Sort sort) {
        this.sort = sort;
    }

    public int getTypeId(){
        return this.typeId;
    }

    public int getSymbolId() {
        return symbolId;
    }

    public void setSymbolId(int symbolId){
        this.symbolId = symbolId;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getRuntimeName(){
        return runtimeName;
    }

    public void setRuntimeName(String runtimeName) {
        this.runtimeName = runtimeName;
    }

    public PLCModifierEnum.Sort getSort(){
        return sort;
    }


    public JsonElement toStringJson() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("symbolId",symbolId);
        jsonObject.addProperty("typeId",typeId);
        jsonObject.addProperty("name",name);
        jsonObject.addProperty("rowNum",rowNum);
        jsonObject.addProperty("columnNum",columnNum);
        String sortName;
        if(sort!=null) sortName = sort.name();
        else sortName="null";
        jsonObject.addProperty("sort",sortName);
        jsonObject.addProperty("runtimeName",runtimeName);
        jsonObject.addProperty("runtimeTypeName",runtimeTypeName);
        //Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //System.out.println("PLCSymbol"+gson.toJson(jsonObject));
       /* return "PLCSymbol{" +
                "symbolId=" + symbolId +
                ", typeId=" + typeId +
                ", name='" + name + '\'' +
                ", rowNum=" + rowNum +
                ", columnNum=" + columnNum +
                ", sort=" + sort +
                ", runtimeName='" + runtimeName + '\'' +
                ", runtimeTypeName='" + runtimeTypeName + '\'' +
                '}';*/
        //组装
        JsonObject jsonSymbol = new JsonObject();
        jsonSymbol.add("PLCSymbol",jsonObject);
        return jsonSymbol;
    }

}
