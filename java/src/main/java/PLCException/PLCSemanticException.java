package PLCException;

//PLC静态语义检查异常
public class PLCSemanticException extends PLCException{
    public PLCSemanticException(){

    }
    public PLCSemanticException(String gripe){
        super(gripe);
    }

}
