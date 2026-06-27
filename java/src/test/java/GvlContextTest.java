import PLCTranslator.GvlContext;
import org.junit.Test;
import java.util.Arrays;
import java.util.Map;
import static org.junit.Assert.*;

public class GvlContextTest {

    private GvlContext gen() { return new GvlContext(); }

    // ─── toNativeType ───

    @Test
    public void testToNativeTypeBasic() {
        GvlContext g = gen();
        assertEquals("INT", g.toNativeType("INT"));
        assertEquals("REAL", g.toNativeType("REAL"));
        assertEquals("BOOL", g.toNativeType("BOOL"));
        assertEquals("STRING", g.toNativeType("STRING"));
        assertEquals("TIME", g.toNativeType("TIME"));
        assertEquals("DINT", g.toNativeType("DINT"));
        assertEquals("LREAL", g.toNativeType("LREAL"));
    }

    @Test
    public void testToNativeTypePLCValue() {
        GvlContext g = gen();
        assertEquals("INT", g.toNativeType("PLC_INT_Value"));
        assertEquals("REAL", g.toNativeType("PLC_Real_Value"));
        assertEquals("BOOL", g.toNativeType("PLC_Bool_Value"));
        assertEquals("STRING", g.toNativeType("PLC_String_Value"));
    }

    @Test
    public void testToNativeTypeUnknown() {
        GvlContext g = gen();
        assertEquals("MyCustomType", g.toNativeType("MyCustomType"));
    }

    @Test
    public void testToNativeTypeNull() {
        GvlContext g = gen();
        assertNull(g.toNativeType(null));
    }

    @Test
    public void testToNativeTypeStruct() {
        GvlContext g = gen();
        GvlContext.StructLayout layout = new GvlContext.StructLayout(
            "MyStruct",
            Arrays.asList(new GvlContext.StructField("X", "INT", 0)),
            2
        );
        g.registerStructType("MyStruct", "PLC_Struct_Value<123>", layout);
        assertEquals("MyStruct", g.toNativeType("PLC_Struct_Value<123>"));
    }

    // ─── getTypeSize ───

    @Test
    public void testGetTypeSize() {
        GvlContext g = gen();
        assertEquals(1, g.getTypeSize("SINT"));
        assertEquals(1, g.getTypeSize("BOOL"));
        assertEquals(2, g.getTypeSize("INT"));
        assertEquals(2, g.getTypeSize("UINT"));
        assertEquals(4, g.getTypeSize("DINT"));
        assertEquals(4, g.getTypeSize("REAL"));
        assertEquals(8, g.getTypeSize("LINT"));
        assertEquals(8, g.getTypeSize("LREAL"));
        assertEquals(8, g.getTypeSize("TIME"));
        assertEquals(256, g.getTypeSize("STRING"));
    }

    @Test
    public void testGetTypeSizeStruct() {
        GvlContext g = gen();
        GvlContext.StructLayout layout = new GvlContext.StructLayout(
            "Point", Arrays.asList(
                new GvlContext.StructField("X", "INT", 0),
                new GvlContext.StructField("Y", "INT", 2)
            ), 4
        );
        g.registerStructType("Point", "PLC_Struct_Value<99>", layout);
        assertEquals(4, g.getTypeSize("Point"));
    }

    @Test
    public void testGetTypeSizeUnknown() {
        GvlContext g = gen();
        assertEquals(4, g.getTypeSize("UnknownType"));
    }

    @Test
    public void testGetTypeSizeNull() {
        GvlContext g = gen();
        assertEquals(4, g.getTypeSize(null));
    }

    // ─── readExpr / writeExpr ───

    @Test
    public void testAllocateOffset() {
        GvlContext g = gen();
        int off = g.allocateOffset("A", "INT");
        assertEquals(0, off);
        assertEquals(Integer.valueOf(0), g.getVarOffset("A"));
        assertEquals("INT", g.getVarType("A"));
    }

    @Test
    public void testAllocateOffsetAlignment() {
        GvlContext g = gen();
        g.allocateOffset("A", "SINT");     // offset 0, size 1
        g.allocateOffset("B", "INT");      // align to 2 → offset 2
        assertEquals(Integer.valueOf(0), g.getVarOffset("A"));
        assertEquals(Integer.valueOf(2), g.getVarOffset("B"));
    }

    // ─── Struct field offset ───

    @Test
    public void testGetStructFieldOffset() {
        GvlContext g = gen();
        GvlContext.StructLayout layout = new GvlContext.StructLayout(
            "Point", Arrays.asList(
                new GvlContext.StructField("X", "INT", 0),
                new GvlContext.StructField("Y", "INT", 2)
            ), 4
        );
        g.registerStructType("Point", "PLC_Struct_Value<99>", layout);
        assertEquals(Integer.valueOf(0), g.getStructFieldOffset("Point", "X"));
        assertEquals(Integer.valueOf(2), g.getStructFieldOffset("Point", "Y"));
    }

    @Test
    public void testGetStructFieldOffsetUnknown() {
        GvlContext g = gen();
        assertNull(g.getStructFieldOffset("Unknown", "X"));
        assertNull(g.getStructFieldOffset("Point", "Z"));
    }

    // ─── I/O Variables ───

    @Test
    public void testRegisterIOVariable() {
        GvlContext g = gen();
        g.registerIOVariable("SW", "BOOL", "%IX0.0");
        assertTrue(g.isIOVariable("SW"));
    }

    @Test
    public void testEmitIOReadInput() {
        GvlContext g = gen();
        g.registerIOVariable("SW", "BOOL", "%IX0.0");
        String read = g.emitIORead("SW");
        assertTrue(read.contains("io.readInputBit(0, 0)"));
    }

    @Test
    public void testEmitIOWriteOutput() {
        GvlContext g = gen();
        g.registerIOVariable("LED", "BOOL", "%QX0.0");
        String write = g.emitIOWrite("LED", "true");
        assertTrue(write.contains("io.writeOutputBit(0, 0, true)"));
    }

    // ─── POU Registration ───

    @Test
    public void testAddProgramName() {
        GvlContext g = gen();
        g.addProgramName("MAIN");
        g.addProgramName("SUB");
        assertEquals(2, g.getProgramNames().size());
        assertEquals("MAIN", g.getProgramNames().get(0));
        assertEquals("SUB", g.getProgramNames().get(1));
    }

    @Test(expected = RuntimeException.class)
    public void testAddProgramNameDuplicate() {
        GvlContext g = gen();
        g.addProgramName("MAIN");
        g.addProgramName("MAIN");
    }

    @Test
    public void testSetFileId() {
        GvlContext g = gen();
        g.setFileId("test");
        assertEquals("test", g.getFileId());
    }

    @Test
    public void testGetFileIdDefault() {
        assertEquals("unnamed", gen().getFileId());
    }

    @Test
    public void testEmitPOURegistration() {
        GvlContext g = gen();
        g.addProgramName("MAIN");
        String code = g.emitPOURegistration("test", g.getProgramNames());
        assertTrue(code.contains("registerPOU_test"));
        assertTrue(code.contains("PROGRAM_MAIN_init"));
        assertTrue(code.contains("PROGRAM_MAIN_cyclic"));
        assertTrue(code.contains("PROGRAM_MAIN_pre"));
        assertTrue(code.contains("PROGRAM_MAIN_post"));
        assertTrue(code.contains("reg.add(\"MAIN\""));
    }

    @Test
    public void testEmitPOURegistrationEmpty() {
        assertEquals("", gen().emitPOURegistration("test", null));
        assertEquals("", gen().emitPOURegistration("test", Arrays.asList()));
    }

    // ─── translateExpr: literal ───

    @Test
    public void testTranslateExprLiteralInt() {
        GvlContext g = gen();
        assertEquals("(2)", g.translateExpr("(*(new INT(2)))"));
    }

    @Test
    public void testTranslateExprLiteralBool() {
        GvlContext g = gen();
        assertEquals("(true)", g.translateExpr("(*(new BOOL(TRUE)))"));
        assertEquals("(false)", g.translateExpr("(*(new BOOL(FALSE)))"));
    }

    @Test
    public void testTranslateExprLiteralReal() {
        GvlContext g = gen();
        assertEquals("(3.14)", g.translateExpr("(*(new REAL(3.14)))"));
    }

    @Test
    public void testTranslateExprNull() {
        assertNull(gen().translateExpr(null));
    }

    @Test
    public void testTranslateExprEmpty() {
        assertEquals("", gen().translateExpr(""));
    }

    // ─── translateExpr: RFM variable → GVL ───

    @Test
    public void testTranslateExprRfmVar() {
        GvlContext g = gen();
        g.allocateOffset("A", "INT");
        g.registerVariable("A", "123");
        String result = g.translateExpr("(*::PLC::RFM->getSymbolByID<INT*>(123))");
        assertEquals("gvl.read<INT>(0)", result);
    }

    @Test
    public void testTranslateExprRfmVarSimple() {
        GvlContext g = gen();
        g.allocateOffset("B", "REAL");
        g.registerVariable("B", "456");
        String result = g.translateExpr("::PLC::RFM->getSymbolByID<REAL*>(456)");
        assertEquals("gvl.read<REAL>(0)", result);
    }

    // ─── translateExpr: RFM function call ───

    @Test
    public void testTranslateExprRfmFuncCall() {
        GvlContext g = gen();
        String result = g.translateExpr("*::PLC::RFM->getSymbolByID<MYFUNC*>(999)->callFunc(1, 2)");
        assertEquals("MYFUNC(1, 2)", result);
    }

    // ─── translateExpr: bare variable → GVL read ───

    @Test
    public void testTranslateExprBareVariable() {
        GvlContext g = gen();
        g.allocateOffset("A", "INT");
        String result = g.translateExpr("A + B");
        assertTrue(result.contains("gvl.read<INT>(0)"));
        assertTrue(result.contains("+"));
    }

    @Test
    public void testTranslateExprBareVariableMultiple() {
        GvlContext g = gen();
        g.allocateOffset("X", "INT");
        g.allocateOffset("Y", "INT");
        String result = g.translateExpr("X * Y");
        assertTrue(result.contains("gvl.read<INT>(0)"));
        assertTrue(result.contains("gvl.read<INT>(2)"));
    }

    // ─── translateExpr: array ───

    @Test
    public void testTranslateExprArrayAccess() {
        GvlContext g = gen();
        g.offsetMap.put("ARR", 0);
        g.typeMap.put("ARR", "ARRAY[5] OF INT");
        String result = g.translateExpr("ARR[I]");
        assertTrue(result.contains("gvl.safeArrayAt<INT>"));
    }

    // ─── getOffsetDefinitions ───

    @Test
    public void testGetOffsetDefinitions() {
        GvlContext g = gen();
        g.allocateOffset("A", "INT");
        g.allocateOffset("B", "REAL");
        String defs = g.getOffsetDefinitions();
        assertTrue(defs.contains("A : INT @ offset 0"));
        assertTrue(defs.contains("B : REAL @ offset 4"));
        assertTrue(defs.contains("Total GVL usage"));
    }

    // ─── getOffsetMap / getTypeMap are unmodifiable ───

    @Test(expected = UnsupportedOperationException.class)
    public void testOffsetMapIsUnmodifiable() {
        gen().getOffsetMap().put("X", 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testTypeMapIsUnmodifiable() {
        gen().getTypeMap().put("X", "INT");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testProgramNamesIsUnmodifiable() {
        gen().getProgramNames().add("X");
    }
}
