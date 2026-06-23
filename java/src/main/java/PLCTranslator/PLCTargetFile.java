package PLCTranslator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class PLCTargetFile {
    static public BufferedWriter targetFile;

    static {
        try {
            targetFile = new BufferedWriter(new FileWriter("STTargetFile.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public void targetFileWrite(String cppCodeStr) throws IOException{
        try {
            targetFile.write(cppCodeStr);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

}
