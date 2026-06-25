import org.junit.Test;
import java.io.File;
import static org.junit.Assert.*;

public class MainMultiInputTest {
    @Test
    public void testMultipleInputFilesGenerateSingleOutput() throws Exception {
        String outputPath = "target/test-output-multi.cpp";
        File output = new File(outputPath);
        if (output.exists()) {
            output.delete();
        }

        String[] args = {"--input", "../examples/multi_input_1.st", "--input", "../examples/multi_input_2.st", "--output", outputPath};
        Main.main(args);
        assertTrue("Output file should be generated", output.exists());
        output.delete();
    }

    @Test
    public void testDuplicateProgramNamesThrows() throws Exception {
        String outputPath = "target/test-output-dup.cpp";
        File output = new File(outputPath);
        if (output.exists()) {
            output.delete();
        }

        String[] args = {"--input", "../examples/multi_input_1.st", "--input", "../examples/duplicate_program.st", "--output", outputPath};
        try {
            Main.main(args);
            fail("Expected RuntimeException due to duplicate PROGRAM name");
        } catch (RuntimeException ex) {
            assertTrue(ex.getMessage().contains("duplication of name"));
        }
    }
}
