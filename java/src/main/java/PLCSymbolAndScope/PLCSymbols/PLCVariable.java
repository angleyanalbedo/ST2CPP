package PLCSymbolAndScope.PLCSymbols;

import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PLCVariable extends PLCSymbol{
    /*
    * variable中的几个属性都需要手动设置
    * */

    public String getUniqueName() {
        StringBuilder sb = new StringBuilder();
        String typeName = PLCTotalSymbolTable.getTypeByTypeID(this.getTypeId()).getName();
        sb.append("(*::PLC::RFM->getSymbolByID<").append(typeName).append("*>(").append(this.symbolId).append("))");
        return new String(sb);
    }

    public String getUniqueNameOfInstance(int instanceSymbolId){
        StringBuilder sb = new StringBuilder();
        String typeName = PLCTotalSymbolTable.getTypeByTypeID(this.getTypeId()).getName();
        sb.append("(*::PLC::RFM->getSymbolByID<").append(typeName).append("*>(").append(instanceSymbolId).append(", ").append(this.symbolId).append("))");
        return new String(sb);
    }

    //变量段类型
    public PLCModifierEnum.VarSections varSections = PLCModifierEnum.VarSections.VAR;

    //访问权限
    public PLCModifierEnum.AccessModifier accessModifier = PLCModifierEnum.AccessModifier.NOT_DECLARED;

    //是否为保留变量
    public PLCModifierEnum.RetainModifier retainQualifiers = PLCModifierEnum.RetainModifier.NON_RETAIN;

    //是否是常量
    public boolean ifConst = false;

    //在声明时变量被赋予的初始值
    // 或者是expr产生的临时变量的值
    //默认为empty
    private String assignVar = "";

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    //locate at
    private String location = "";

    public void setIfConst(boolean ifConst){
        this.ifConst = ifConst;
    }

    public boolean getIfConst(){
        return this.ifConst;
    }

    public void setVarSections(PLCModifierEnum.VarSections varSections) {
        this.varSections = varSections;
    }

    public PLCModifierEnum.VarSections getVarSections() {
        return varSections;
    }

    public void setRetainQualifiers(PLCModifierEnum.RetainModifier retainQualifiers) {
        this.retainQualifiers = retainQualifiers;
    }

    public void setRetainQualifiers(String retainQualifiers) {
        this.retainQualifiers = PLCModifierEnum.RetainModifier.valueOf(retainQualifiers);
    }

    public PLCModifierEnum.RetainModifier getRetainQualifiers() {
        return retainQualifiers;
    }

    public void setAccessModifier(PLCModifierEnum.AccessModifier accessModifier) {
        this.accessModifier = accessModifier;
    }

    public PLCModifierEnum.AccessModifier getAccessModifier() {
        return accessModifier;
    }

    public PLCTypeDeclSymbol getDeclSymbol() {
        return declSymbol;
    }

    public void setDeclSymbol(PLCTypeDeclSymbol declSymbol) {
        this.declSymbol = declSymbol;
    }

    //变量符号对应的声明的符号
    private PLCTypeDeclSymbol declSymbol;

    public PLCVariable(){
        super();
    }

    public PLCVariable(PLCVariable another) {
        this.typeId = another.typeId;
        this.sort = another.sort;
        this.varSections = another.varSections;
        this.accessModifier = another.accessModifier;
        this.retainQualifiers = another.retainQualifiers;
        this.ifConst = another.ifConst;
        this.assignVar = another.assignVar;
        this.location = another.location;
        this.declSymbol = another.declSymbol;
    }

    //
    public String getAssignVar() {
        return assignVar;
    }

    public void setAssignVar(String assignVar) {
        this.assignVar = assignVar;
    }


//    @Override
//    public void setName(String name) {
//        super.setName(name);
//
//        PLCModifierEnum.Sort currentScopeSort = PLCScopeStack.currentScope.getDeclSymbol().getSort();
//        switch (currentScopeSort){
//            case FC_DECL, METHOD_DECL -> {
//                this.runtimeName = "* " + PLCScopeStack.currentScope.getScopeLocation() + "->"+name;
//            }
//            default -> {
//                this.runtimeName = "* " + name;
//            }
//        }
//    }

    @Override
    public String toString() {
        return "PLCVariable{" +
                "varSections=" + varSections +
                ", accessModifier=" + accessModifier +
                ", retainQualifiers=" + retainQualifiers +
                ", ifConst=" + ifConst +
                ", assignVar='" + assignVar + '\'' +
                ", location='" + location + '\'' +
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

        jsonObject.addProperty("varSections", varSections.name());
        jsonObject.addProperty("accessModifier", accessModifier.name());
        jsonObject.addProperty("retainQualifiers", retainQualifiers.name());
        jsonObject.addProperty("ifConst", ifConst);
        jsonObject.addProperty("assignVar", assignVar);
        jsonObject.addProperty("location", location);
        jsonObject.addProperty("symbolId", symbolId);
        jsonObject.addProperty("typeId", typeId);
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("rowNum", rowNum);
        jsonObject.addProperty("columnNum", columnNum);
        jsonObject.addProperty("sort", sort!=null?sort.name():null);
        jsonObject.addProperty("runtimeName", runtimeName);
        jsonObject.addProperty("runtimeTypeName", runtimeTypeName);
        JsonObject jsonSymbol = new JsonObject();
        jsonSymbol.add("PLCVariable",jsonObject);
        return jsonSymbol;
    }

//    @Override
//    public String getRuntimeName() {
//        return "*" + getName();
//    }
}
