package PLCException;

//PLC语法异常
public class PLCParserException extends PLCException{

    PLCParserException(){

    }

    PLCParserException(String gripe){
        super(gripe);
    }
}
