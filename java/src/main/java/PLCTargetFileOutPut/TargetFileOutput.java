package PLCTargetFileOutPut;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class TargetFileOutput {

    static FileWriter targetFile;

    static {
        try {
            targetFile = new FileWriter("src/main/resources/output/main.cpp");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static BufferedWriter outputWriter = new BufferedWriter(targetFile);

    public TargetFileOutput() throws IOException {

    }

    static public void writeSentence(String outputSentence) throws IOException {
        outputWriter.write(outputSentence);
    }

    static public void writeTarget(String readSentence){
        try {
            writeSentence(readSentence);
        }catch (IOException e){
            System.out.println("TargetFile Error");
        }
    }

    static public void closeBufferAndFileWriter() throws IOException {
        outputWriter.close();
        targetFile.close();
    }

    static public void closeWriter(){
        try {
            closeBufferAndFileWriter();
        }catch (IOException e){
            System.out.println("Close Writer Error");
        }
    }

}
