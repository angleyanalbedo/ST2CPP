import PLCSymbolAndScope.CompilerState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SyntaxCoverageTest {

    private static final File SYNTAX_DIR = new File("../examples/projects/syntax_tests");
    private static final File OUT_DIR = new File("../output/flat/build");

    private final File stFile;
    private final String stem;

    public SyntaxCoverageTest(File stFile, String stem) {
        this.stFile = stFile;
        this.stem = stem;
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() {
        List<Object[]> params = new ArrayList<>();
        File[] files = SYNTAX_DIR.listFiles((dir, name) ->
            name.endsWith(".st") && !name.startsWith("__"));
        if (files != null) {
            for (File f : files) {
                String stem = f.getName().replace(".st", "");
                params.add(new Object[]{ f, stem });
            }
        }
        return params;
    }

    @Before
    public void setUp() {
        CompilerState.reset();
        OUT_DIR.mkdirs();
    }

    @Test
    public void testCompileSuccess() {
        File outFile = new File(OUT_DIR, stem + ".cpp");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capture));

        try {
            Main.main(new String[]{
                "--input", stFile.getAbsolutePath(),
                "--output", outFile.getAbsolutePath(),
                "--file-id", stem
            });
        } catch (Exception e) {
            fail("Compiler threw for " + stem + ": " + e.getMessage());
        } finally {
            System.setOut(originalOut);
        }

        assertTrue("Output file not generated for " + stem, outFile.exists());
    }

    @After
    public void tearDown() {
        CompilerState.reset();
    }
}
