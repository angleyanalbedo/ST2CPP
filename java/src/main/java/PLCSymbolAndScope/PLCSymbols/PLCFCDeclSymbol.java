package PLCSymbolAndScope.PLCSymbols;

public class PLCFCDeclSymbol extends PLCBaseFUNDeclSymbol {
    public PLCFCDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.FC_DECL;
    }
    public PLCFCDeclSymbol(String name, int rowNum){
        super(name, rowNum);
        sort = PLCModifierEnum.Sort.FC_DECL;
    }

    public PLCFCDeclSymbol(PLCFCDeclSymbol resource){
        super(resource);
    }
}
