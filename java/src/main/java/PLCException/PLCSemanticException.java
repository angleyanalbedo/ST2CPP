package PLCException;

import org.antlr.v4.runtime.ParserRuleContext;

public class PLCSemanticException extends PLCException{
    private ParserRuleContext ctx;

    public PLCSemanticException(){
    }

    public PLCSemanticException(String gripe){
        super(gripe);
    }

    public PLCSemanticException(String gripe, ParserRuleContext ctx){
        super(gripe);
        this.ctx = ctx;
    }

    public ParserRuleContext getCtx() {
        return ctx;
    }
}
