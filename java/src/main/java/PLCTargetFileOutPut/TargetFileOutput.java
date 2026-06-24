package PLCTargetFileOutPut;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class TargetFileOutput {

    static FileWriter targetFile;
    static BufferedWriter outputWriter;
    static String outputPath = "src/main/resources/output/main.cpp";

    /**
     * 设置输出文件路径（必须在首次 writeTarget 之前调用）
     */
    static public void setOutputPath(String path) {
        outputPath = path;
    }

    /**
     * 初始化输出文件（懒加载，首次 writeTarget 时自动调用）
     */
    static private void ensureOpen() {
        if (outputWriter == null) {
            try {
                targetFile = new FileWriter(outputPath);
                outputWriter = new BufferedWriter(targetFile);
            } catch (IOException e) {
                throw new RuntimeException("Cannot open output file: " + outputPath, e);
            }
        }
    }

    static public void writeSentence(String outputSentence) throws IOException {
        ensureOpen();
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
        if (outputWriter != null) {
            outputWriter.close();
        }
        if (targetFile != null) {
            targetFile.close();
        }
    }

    static public void closeWriter(){
        try {
            closeBufferAndFileWriter();
        }catch (IOException e){
            System.out.println("Close Writer Error");
        }
    }

}
