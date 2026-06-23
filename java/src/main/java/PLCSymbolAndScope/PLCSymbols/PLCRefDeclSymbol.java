package PLCSymbolAndScope.PLCSymbols;

import static PLCSymbolAndScope.PLCSymbols.PLCModifierEnum.Sort.REF;
import static PLCSymbolAndScope.PLCSymbols.PLCModifierEnum.Sort.REF_DECL;

public class PLCRefDeclSymbol extends PLCTypeDeclSymbol{
    public PLCRefDeclSymbol(){
        super();
        this.sort = REF_DECL;
        this.varSort = REF;
    }

    public PLCRefDeclSymbol(PLCRefDeclSymbol resource){
        super(resource);
        this.referredTypeId = resource.referredTypeId;
        this.sort = REF_DECL;
        this.varSort = REF;
    }

    public int getReferredTypeId() {
        return referredTypeId;
    }

    public void setReferredTypeId(int referredTypeId) {
        this.referredTypeId = referredTypeId;
    }

    private int referredTypeId;

}
