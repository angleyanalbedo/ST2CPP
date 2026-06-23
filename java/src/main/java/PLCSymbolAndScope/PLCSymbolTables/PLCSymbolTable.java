package PLCSymbolAndScope.PLCSymbolTables;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCSymbols.PLCImportScopeTypeDeclType;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;

//原subtable
public class PLCSymbolTable {

    //符号表的ID,在构造方法中初始化
    private int tableId;

    //引入此表的符号对象，在入栈时设置
    private PLCImportScopeTypeDeclType srcSymbol;

    //符号表所属作用域 -- 对应作用域,在入栈时设置
    private PLCScope tableScope;

    //该表下所有符号（按名称存储）,调用方法填充,
    public HashMap<String, PLCSymbol> symbolNameHashMap = new HashMap<>();

    public HashMap<Integer, PLCSymbol> getSymbolIDHashMap() {
        return symbolIDHashMap;
    }

    //该表下所有符号（按ID存储）,调用方法填充
    public HashMap<Integer, PLCSymbol> symbolIDHashMap = new HashMap<>();

    //默认构造方法
    public PLCSymbolTable(){
        //设置符号表ID
        this.tableId = IDGenerator.getIDGenerator().newTableId();
    }

    //在此符号表下搜索符号,不应当在visit中调用
    public PLCSymbol findSymbol(String name){
        return this.symbolNameHashMap.get(name);
    }

    public PLCSymbol findSymbol(String name, PLCModifierEnum.Sort sort){
        ArrayList<PLCSymbol> sameNamedSymbol = this.findSameNamedSymbol(name);
        for (PLCSymbol symbol : sameNamedSymbol) {
            if(symbol.getSort() == sort){
                return symbol;
            }
        }
        return null;
    }

    //寻找重名的符号
    public ArrayList<PLCSymbol> findSameNamedSymbol(String name){
        ArrayList<PLCSymbol> sameNameSymbols = new ArrayList<>();
        for (PLCSymbol value : this.symbolIDHashMap.values()) {
            if(name.equals(value.getName())){
                sameNameSymbols.add(value);
            }
        }
        return sameNameSymbols;
    }

    //向符号表的哈希表内添加符号, 一次添加两个
    public void addSymbol(PLCSymbol symbol, int symbolId, String symbolName){
        this.symbolIDHashMap.put(symbolId, symbol);
        this.symbolNameHashMap.put(symbolName, symbol);
    }

    /**
     *需要提前将符号信息填写完整*/
    public void addSymbol(PLCSymbol plcSymbol){
        this.symbolNameHashMap.put(plcSymbol.name, plcSymbol);
        this.symbolIDHashMap.put(plcSymbol.getSymbolId(), plcSymbol);
    }


    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("Table Id:").append(this.tableId).append("\n").
                append("Symbol name:").append(this.getSrcSymbol().name).append("\n").
                append("Symbol list : ").append("\n").
                append("Symbol Sort : ").append(this.getSrcSymbol().getSort()).append("\n");
        for (PLCSymbol symbol : this.symbolIDHashMap.values()) {
            str.append(symbol.toStringJson()).append("\n");
        }
        str.append("*******************************\n");
        return new String(str);
    }

    public JsonElement toJsonString(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Table Id",this.tableId);
        jsonObject.addProperty("Symbol name",this.getSrcSymbol().name);
        jsonObject.addProperty("Symbol list","");
        String sortName;
        if(this.getSrcSymbol().getSort()!=null) sortName = this.getSrcSymbol().getSort().name();
        else sortName="null";
        jsonObject.addProperty("Symbol Sort",sortName);

        JsonArray jsonArray = new JsonArray();
        for (PLCSymbol symbol : this.symbolIDHashMap.values()) {
            jsonArray.add(symbol.toStringJson());
        }
        jsonObject.add("PLCSymbolMap",jsonArray);
        return jsonObject;
    }

    //-------------------------------以下为get set方法---------------------------------
    //引入此表的符号对象的set get方法
    public void setSrcSymbol(PLCImportScopeTypeDeclType srcSymbol) {
        this.srcSymbol = srcSymbol;
    }

    public PLCImportScopeTypeDeclType getSrcSymbol() {
        return srcSymbol;
    }

    //对应作用域的get set方法
    public void setTableScope(PLCScope tableScope) {
        this.tableScope = tableScope;
    }

    public PLCScope getTableScope() {
        return tableScope;
    }

    //tableId的get set方法

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public int getTableId() {
        return tableId;
    }

}
