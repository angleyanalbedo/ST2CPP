import PLCSymbolAndScope.CompilerState;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class SnapshotTest {

    private static final File SNAPSHOT_DIR = new File("src/test/resources/snapshots");
    private static final boolean UPDATE_MODE = "true".equalsIgnoreCase(
        System.getProperty("updateSnapshots", "false"));

    private final File stFile;
    private final File goldenFile;
    private final String stem;

    public SnapshotTest(File stFile, File goldenFile, String stem) {
        this.stFile = stFile;
        this.goldenFile = goldenFile;
        this.stem = stem;
    }

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> data() {
        List<Object[]> params = new ArrayList<>();
        File[] files = SNAPSHOT_DIR.listFiles((dir, name) -> name.endsWith(".st"));
        if (files != null) {
            for (File f : files) {
                String stem = f.getName().replace(".st", "");
                params.add(new Object[]{ f, new File(SNAPSHOT_DIR, stem + ".cpp"), stem });
            }
        }
        return params;
    }

    @Test
    public void testSnapshot() throws Exception {
        File tmp = File.createTempFile("snapshot-", ".cpp");
        tmp.deleteOnExit();

        // 重定向 stdout 以抑制 Main 的输出
        PrintStream originalOut = System.out;
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capture));

        try {
            Main.main(new String[]{
                "--input", stFile.getAbsolutePath(),
                "--output", tmp.getAbsolutePath(),
                "--file-id", stem
            });
        } finally {
            System.setOut(originalOut);
        }

        assertTrue("Output file not generated", tmp.exists());

        String actual = new String(Files.readAllBytes(tmp.toPath()))
            .replace("\r\n", "\n");

        if (UPDATE_MODE) {
            Files.write(goldenFile.toPath(), actual.getBytes());
            System.out.println("  [UPDATED] " + goldenFile.getName());
            tmp.delete();
            return;
        }

        assertTrue("Golden file not found: " + goldenFile
            + "\nRun with -DupdateSnapshots=true to create it.", goldenFile.exists());

        String expected = new String(Files.readAllBytes(goldenFile.toPath()))
            .replace("\r\n", "\n");

        assertEquals("Snapshot mismatch: " + stem, expected, actual);
        tmp.delete();
    }
}
