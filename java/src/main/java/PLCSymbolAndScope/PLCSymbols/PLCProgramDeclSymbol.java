package PLCSymbolAndScope.PLCSymbols;

public class PLCProgramDeclSymbol extends PLCImportScopeTypeDeclType {
    public PLCProgramDeclSymbol(){
        super();
        sort = PLCModifierEnum.Sort.PROGRAM_DECL;
        varSort = PLCModifierEnum.Sort.PROGRAM;
    }

    public PLCProgramDeclSymbol(String name, int rowNum){
        super(name, rowNum);
        sort = PLCModifierEnum.Sort.PROGRAM_DECL;
        varSort = PLCModifierEnum.Sort.PROGRAM;
    }

    public PLCProgramDeclSymbol(PLCProgramDeclSymbol resource){
        super(resource);
    }

}
