package PLCException;

//PLC词法异常
public class PLCLexerException extends  PLCException{

    PLCLexerException(){

    }
    PLCLexerException(String gripe){
        super(gripe);
    }
}
