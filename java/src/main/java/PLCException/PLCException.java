package PLCException;

import java.io.IOException;


//PLC异常类继承IOException
public class PLCException extends RuntimeException {
    PLCException(){

    }

    //带详细描述信息的构造器
    PLCException(String gripe){
        super(gripe);
    }

}
