package PLCSymbolAndScope.PLCSymbols;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static PLCSymbolAndScope.PLCSymbols.PLCModifierEnum.ClassModifier;

public class PLCMethodDeclSymbol extends PLCBaseFUNDeclSymbol{
    //方法的修饰符
    public PLCModifierEnum.ClassModifier methodModifier = ClassModifier.NONE;

    public void setMethodModifier(String methodModifier){
        this.methodModifier = PLCModifierEnum.ClassModifier.valueOf(methodModifier);
    }

    public PLCModifierEnum.ClassModifier getMethodModifier() {
        return methodModifier;
    }

    public boolean getIfAbstract(){
        return this.methodModifier == ClassModifier.ABSTRACT;
    }

    //访问修饰符
    public  PLCModifierEnum.AccessModifier accessModifier = PLCModifierEnum.AccessModifier.PROTECTED;

    public void setAccessModifier(String accessModifier){
        this.accessModifier = PLCModifierEnum.AccessModifier.valueOf(accessModifier);
    }

    public boolean isIfOverride() {
        return ifOverride;
    }

    public void setIfOverride(boolean ifOverride) {
        this.ifOverride = ifOverride;
    }

    //是否重写
    private boolean ifOverride;

    //重写的符号 如果实现困难就不做这个了
    public PLCMethodDeclSymbol overrideMethod;

    public PLCMethodDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.METHOD_DECL;
    }

    public PLCMethodDeclSymbol(String name, int rowNum){
        super(name, rowNum);
        sort = PLCModifierEnum.Sort.METHOD_DECL;
    }

    public PLCMethodDeclSymbol(PLCMethodDeclSymbol resource){
        super(resource);
    }

    @Override
    public String toString() {
        return "PLCMethodDeclSymbol{" +
                "methodModifier=" + methodModifier +
                ", accessModifier=" + accessModifier +
                ", ifOverride=" + ifOverride +
                ", overrideMethod=" + overrideMethod +
                ", returnTypeId=" + returnTypeId +
                ", returnVarSort=" + returnVarSort +
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
                ", accessVar='" + accessVars + '\'' +
                '}';
    }

    public JsonElement toStringJson() {

        JsonObject jsonObject = new JsonObject();
        String name;
        if (methodModifier!=null) name=methodModifier.toString();
        else name="null";
        jsonObject.addProperty("methodModifier", name);
        if (accessModifier!=null) name=accessModifier.toString();
        else name="null";
        jsonObject.addProperty("accessModifier", name);
        jsonObject.addProperty("ifOverride", ifOverride);
        jsonObject.add("overrideMethod", overrideMethod!=null ? overrideMethod.toStringJson() : null);
        jsonObject.addProperty("returnTypeId", returnTypeId);
        if (returnVarSort!=null) name=returnVarSort.toString();
        else name="null";
        jsonObject.addProperty("returnVarSort", name);
        JsonArray NamespaceDeclArray = new JsonArray();
        for (PLCNamespaceDeclSymbol plcNamespaceDeclSymbol : namespaces ) {
            NamespaceDeclArray.add( plcNamespaceDeclSymbol.toStringJson());
        }
        jsonObject.add("namespaces",NamespaceDeclArray);
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
        JsonArray accessVarArray = new JsonArray();
        for (PLCVariable plcVariable : accessVars ) {
            accessVarArray.add( plcVariable.toStringJson());
        }
        jsonObject.add("accessVars",accessVarArray);
        JsonObject jsonSymbol = new JsonObject();
        jsonSymbol.add("PLCMethodDeclSymbol",jsonObject);
        return jsonSymbol;
    }
}

