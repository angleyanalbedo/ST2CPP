package PLCSymbolAndScope.PLCSymbols;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

public class PLCInterfaceDeclSymbol extends PLCImportScopeTypeDeclType implements AbstractMethod, UsingNamespace{
    //接口继承的接口
    public ArrayList<PLCInterfaceDeclSymbol> baseInterfaces = new ArrayList<>();

    private ArrayList<String> getBaseInterfacesName(){
        ArrayList<String> names = new ArrayList<>();
        for (PLCInterfaceDeclSymbol baseInterface : this.baseInterfaces) {
            names.add(baseInterface.getName());
        }
        return names;
    }

    //接口使用的命名空间
    private ArrayList<PLCNamespaceDeclSymbol> namespaceList = new ArrayList<>();

    public void addInterface(PLCInterfaceDeclSymbol interfaceDeclSymbol){
        this.baseInterfaces.add(interfaceDeclSymbol);
    }

    public PLCInterfaceDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.INTERFACE_DECL;
    }


    public PLCInterfaceDeclSymbol(String name, int rowNum){
        super(name, rowNum);
        sort = PLCModifierEnum.Sort.INTERFACE_DECL;
    }

    public PLCInterfaceDeclSymbol(PLCInterfaceDeclSymbol resource){
        super(resource);
    }

    public void addBaseInterfaces(PLCInterfaceDeclSymbol interfaceDeclSymbol){
        this.baseInterfaces.add(interfaceDeclSymbol);
    }

    //待实现的方法 = 父类的abstract方法和实现的接口的所有方法
    private ArrayList<PLCMethodDeclSymbol> abstractMethods = new ArrayList<>();

    @Override
    public void addAbstractMethod(PLCMethodDeclSymbol method) {
        this.abstractMethods.add(method);
    }

    @Override
    public void addAllAbsMethods(ArrayList<PLCMethodDeclSymbol> methods) {
        this.abstractMethods.addAll(methods);
    }

    @Override
    public ArrayList<PLCMethodDeclSymbol> getAbstractMethods() {
        return this.abstractMethods;
    }

    @Override
    public ArrayList<PLCNamespaceDeclSymbol> getNamespaces() {
        return this.namespaceList;
    }

    @Override
    public void addNameSpace(PLCNamespaceDeclSymbol namespaceDeclSymbol) {
        namespaceList.add(namespaceDeclSymbol);
    }

    @Override
    public String toString() {
        return "PLCInterfaceDeclSymbol{" +
//                "baseInterfaces=" + baseInterfaces +
                ", namespaceList=" + namespaceList +
                ", abstractMethods=" + abstractMethods +
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
        JsonArray NamespaceDeclArray = new JsonArray();
        for (PLCNamespaceDeclSymbol plcNamespaceDeclSymbol : namespaceList ) {
            NamespaceDeclArray.add( plcNamespaceDeclSymbol.toStringJson());
        }
        jsonObject.add("namespaceList",NamespaceDeclArray);
        JsonArray MethodDeclArray = new JsonArray();
        for (PLCMethodDeclSymbol plcMethodDeclSymbol : abstractMethods ) {
            MethodDeclArray.add( plcMethodDeclSymbol.toStringJson());
        }
        jsonObject.add("abstractMethods",MethodDeclArray);
        JsonObject jsonSymbol = new JsonObject();
        jsonSymbol.add("PLCInterfaceDeclSymbol",jsonObject);
        return jsonSymbol;
    }
}
