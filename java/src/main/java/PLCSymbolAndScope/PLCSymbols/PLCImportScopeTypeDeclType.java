package PLCSymbolAndScope.PLCSymbols;

import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;

//引入作用域的符号 IS:import Scope
public class PLCImportScopeTypeDeclType extends PLCTypeDeclSymbol{

    public PLCImportScopeTypeDeclType(PLCImportScopeTypeDeclType resource){
        super(resource);
        this.importScope = resource.importScope;
        this.importSymbolTable = resource.importSymbolTable;
    }


    public PLCImportScopeTypeDeclType(){
        super();
    }
    public PLCImportScopeTypeDeclType(String name, int rowNum){
        super(name, rowNum);
    }

    //该符号引入的符号表，在入栈时自动设置
    public PLCSymbolTable importSymbolTable;

    public void setImportSymbolTable(PLCSymbolTable importSymbolTable) {
        this.importSymbolTable = importSymbolTable;
    }

    public PLCSymbolTable getImportSymbolTable() {
        return importSymbolTable;
    }

    //该符号引入的作用域，在入栈时自动设置
    public PLCScope importScope;

    public void setImportScope(PLCScope importScope) {
        this.importScope = importScope;
    }

    public PLCScope getImportScope() {
        return importScope;
    }

    @Override
    public String toString() {
        return "PLCImportScopeTypeDeclType{" +
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
