package PLCSymbolAndScope.PLCSymbols;

public class PLCClassDeclSymbol extends PLCBaseClassDeclSymbol{

    public PLCClassDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.CLASS_DECL;
        varSort = PLCModifierEnum.Sort.CLASS;
        this.runtimeName = "";
    }

    public PLCClassDeclSymbol(PLCClassDeclSymbol resource){
        super(resource);
    }

}
