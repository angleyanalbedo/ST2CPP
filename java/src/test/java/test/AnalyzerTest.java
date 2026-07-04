package test;

import java.nio.file.*;
import java.util.*;
import com.st2c.lsp.StDeclarationScanner;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class AnalyzerTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        // Default to the robot_arm example if no path provided
        String targetDir = args.length > 0 ? args[0] : "../examples/projects/robot_arm";
        Path root = Paths.get(targetDir).toAbsolutePath().normalize();
        
        System.out.println("Scanning directory: " + root);
        if (!Files.isDirectory(root)) {
            System.err.println("Error: Directory does not exist.");
            System.exit(1);
        }

        List<String> files = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".st"))
                  .forEach(p -> files.add(p.toString()));
        }
        
        if (files.isEmpty()) {
            System.out.println("No .st files found in directory.");
            return;
        }

        System.out.println("\n====== 1. SCAN RESULTS (Defines & References) ======");
        Method m_scan = StDeclarationScanner.class.getDeclaredMethod("scan", String.class, String.class);
        m_scan.setAccessible(true);
        
        java.util.function.Function<String, String> contentProvider = path -> {
            try { return new String(Files.readAllBytes(Paths.get(path))); } catch(Exception e) { return null; }
        };
        
        Map<String, Object> infoMap = new HashMap<>();
        Map<String, String> symbolToFile = new HashMap<>();
        
        for (String f : files) {
            String content = contentProvider.apply(f);
            Object info = m_scan.invoke(null, content, f);
            infoMap.put(f, info);
            
            Field defField = info.getClass().getDeclaredField("defines");
            defField.setAccessible(true);
            Map<String, Object> defines = (Map<String, Object>) defField.get(info);
            
            Field refField = info.getClass().getDeclaredField("references");
            refField.setAccessible(true);
            Set<String> references = (Set<String>) refField.get(info);
            
            for (String sym : defines.keySet()) {
                symbolToFile.put(sym.toLowerCase(), f);
            }
            
            System.out.println("\nFILE: " + Paths.get(f).getFileName().toString());
            System.out.println("  Defines: " + defines.keySet());
            System.out.println("  References: " + references);
        }
        
        System.out.println("\n====== 2. DEPENDENCY GRAPH ======");
        for (String f : files) {
            Object info = infoMap.get(f);
            Field refField = info.getClass().getDeclaredField("references");
            refField.setAccessible(true);
            Set<String> refs = (Set<String>) refField.get(info);
            
            Set<String> fileDeps = new HashSet<>();
            for (String ref : refs) {
                String definingFile = symbolToFile.get(ref.toLowerCase());
                if (definingFile != null && !definingFile.equals(f)) {
                    fileDeps.add(Paths.get(definingFile).getFileName().toString());
                }
            }
            System.out.println(Paths.get(f).getFileName().toString() + " depends on: " + fileDeps);
        }
        
        System.out.println("\n====== 3. TOPOLOGICAL SORT ORDER ======");
        Method m_sort = StDeclarationScanner.class.getDeclaredMethod("topologicalSortWithDetails", List.class, java.util.function.Function.class);
        m_sort.setAccessible(true);
        
        Object sortResult = m_sort.invoke(null, files, contentProvider);
        Field sfField = sortResult.getClass().getDeclaredField("sortedFiles");
        sfField.setAccessible(true);
        List<String> sorted = (List<String>) sfField.get(sortResult);
        
        for (int i = 0; i < sorted.size(); i++) {
            System.out.println(String.format("  %d. %s", (i + 1), Paths.get(sorted.get(i)).getFileName().toString()));
        }
    }
}
