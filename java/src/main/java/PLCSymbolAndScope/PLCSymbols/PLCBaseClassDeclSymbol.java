package PLCSymbolAndScope.PLCSymbols;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

//派生出方法块和类
public class PLCBaseClassDeclSymbol extends PLCImportScopeTypeDeclType implements AbstractMethod, UsingNamespace, DeclareVariable, DeclareMethod{
    public PLCBaseClassDeclSymbol(PLCBaseClassDeclSymbol resource){
        super(resource);
    }


    public ArrayList<PLCInterfaceDeclSymbol> getInterfaces() {
        return interfaces;
    }

    @Override
    public ArrayList<PLCNamespaceDeclSymbol> getNamespaces() {
        return namespaces;
    }

    //类实现的接口
    public ArrayList<PLCInterfaceDeclSymbol> interfaces = new ArrayList<>();

    //类使用的命名空间
    public ArrayList<PLCNamespaceDeclSymbol> namespaces = new ArrayList<>();

    //类内变量列表
    private final HashMap<String, PLCVariable> variableMap = new HashMap<>();

    //待实现的方法 = 父类的abstract方法和实现的接口的所有方法
    private final ArrayList<PLCMethodDeclSymbol> abstractMethods = new ArrayList<>();

    //类内的方法表
    private final ArrayList<PLCMethodDeclSymbol> methodMap = new ArrayList<>();

    public PLCBaseClassDeclSymbol getBaseClass() {
        return baseClass;
    }

    public void setBaseClass(PLCBaseClassDeclSymbol baseClass) {
        this.baseClass = baseClass;
    }

    //指向基类类型的定义符号对象
    protected PLCBaseClassDeclSymbol baseClass;

    public PLCModifierEnum.ClassModifier getClassModifier() {
        return classModifier;
    }

    public void setClassModifier(PLCModifierEnum.ClassModifier classModifier) {
        this.classModifier = classModifier;
    }

    public void setClassModifier(String classModifier) {
        this.classModifier = PLCModifierEnum.ClassModifier.valueOf(classModifier);
    }

    //class是否是abstract或者是final
    public PLCModifierEnum.ClassModifier classModifier;

    public PLCBaseClassDeclSymbol(){
        super();
    }
    public PLCBaseClassDeclSymbol(String name, int rowNum){
        super(name, rowNum);
    }

    public void addInterface(PLCInterfaceDeclSymbol interfaz){
        this.interfaces.add(interfaz);
    }

    /**
     * 在基类和本类中查找方法
     * */
    public PLCBaseFUNDeclSymbol findMethod(String name){
        for (PLCMethodDeclSymbol methodDeclSymbol : this.methodMap) {
            if(name.equals(methodDeclSymbol.getName())){
                return methodDeclSymbol;
            }
        }
        return null;
    }

    /**
     * 根据名称和参数寻找类中的方法
     * */
    public PLCBaseFUNDeclSymbol findMethod(String name, ArrayList<PLCVariable> params){
        //在本层查找
        ArrayList<String> paramsNameList = new ArrayList<>();
        for (PLCVariable param : params) {
            paramsNameList.add(param.getName());
        }

        ArrayList<PLCSymbol> sameNamedMethods = this.importSymbolTable.findSameNamedSymbol(name);
        for (PLCSymbol method : sameNamedMethods) {
            if(method.getSort() != PLCModifierEnum.Sort.METHOD_DECL){
                continue;
            }
            PLCBaseFUNDeclSymbol function = (PLCBaseFUNDeclSymbol) method;

            //参数名称检查是否有问题
            boolean flag = false;

            for (PLCVariable param : params) {
                String paramName = param.getName();
                if(paramName == null){
                    throw new PLCSemanticException("不支持不正规调用");
                }
                PLCVariable accessVar = function.getAccessVar(paramName);
                //名称检查
                if(accessVar == null){
                    flag = true;
                    break;
                }

                //变量段检查
                PLCModifierEnum.VarSections paramSection = param.getVarSections();
                if(accessVar.getVarSections() != paramSection){
                    flag = true;
                    break;
                }

                //类型检查
                PLCTypeDeclSymbol varType = PLCTotalSymbolTable.getTypeByTypeID(accessVar.getTypeId());
                if(!varType.checkCanAssignWith(param.getTypeId())){
                    flag = true;
                    break;
                }
            }
            //如果上述检查没通过，则直接跳过检查下一个
            if(flag){
                continue;
            }

            //数量检查
            flag = true;
            ArrayList<PLCVariable> accessVars = function.getAccessVars();
            for (PLCVariable accessVar : accessVars) {
                String accessVarName = accessVar.getName();
                if(!paramsNameList.contains(accessVarName) && accessVar.getAssignVar() == null){
                    flag = false;
                    break;
                }
            }
            if(flag){
                return function;
            }
        }
        return null;
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
        variableMap.put(var.getName(), var);
    }

    @Override
    public void addAllVariable(Collection<PLCVariable> vars) {
        for (PLCVariable var : vars) {
            this.addVariable(var);
        }
    }


    @Override
    public void addNameSpace(PLCNamespaceDeclSymbol namespaceDeclSymbol){
        this.namespaces.add(namespaceDeclSymbol);
    }

    @Override
    public void addAbstractMethod(PLCMethodDeclSymbol method){
        this.abstractMethods.add(method);
    }

    @Override
    public void addAllAbsMethods(ArrayList<PLCMethodDeclSymbol> methods){
        this.abstractMethods.addAll(methods);
    }

    @Override
    public ArrayList<PLCMethodDeclSymbol> getAbstractMethods(){
        return this.abstractMethods;
    }

    @Override
    public void addMethod(PLCMethodDeclSymbol method) {
        methodMap.add(method);
    }

    @Override
    public void addAllMethods(ArrayList<PLCMethodDeclSymbol> methods) {
        methodMap.addAll(methods);
    }

    @Override
    public ArrayList<PLCMethodDeclSymbol> getMethods() {
        return this.methodMap;
    }



    @Override
    public String toString() {
        return "PLCBaseClassDeclSymbol{" +
                "interfaces=" + interfaces +
                ", namespaces=" + namespaces +
                ", abstractMethods=" + abstractMethods +
                ", baseClass=" + baseClass +
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

        JsonArray interfaceArray = new JsonArray();
        for (PLCInterfaceDeclSymbol plcInterfaceDeclSymbol: interfaces ) {
            interfaceArray.add( plcInterfaceDeclSymbol.toStringJson());
        }
        jsonObject.add("interfaces", interfaceArray);
        JsonArray NamespaceDeclArray = new JsonArray();
        for (PLCNamespaceDeclSymbol plcNamespaceDeclSymbol : namespaces ) {
            NamespaceDeclArray.add( plcNamespaceDeclSymbol.toStringJson());
        }
        jsonObject.add("namespaces",NamespaceDeclArray);
        JsonArray MethodDeclArray = new JsonArray();
        for (PLCMethodDeclSymbol plcMethodDeclSymbol : abstractMethods ) {
            MethodDeclArray.add( plcMethodDeclSymbol.toStringJson());
        }
        jsonObject.add("abstractMethods",MethodDeclArray);
        jsonObject.add("baseClass", baseClass!=null? baseClass.toStringJson():null);
        jsonObject.addProperty("classModifier", classModifier!=null? classModifier.name():null);
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
        jsonSymbol.add("PLCBaseClassDeclSymbol",jsonObject);
        return jsonSymbol;
    }
}
