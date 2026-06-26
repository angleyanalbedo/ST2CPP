import PLCTranslator.CodeGenerator;
import PLCTranslator.FlatCodeGenerator;
import org.junit.Test;
import java.util.Arrays;
import java.util.Map;
import static org.junit.Assert.*;

public class FlatCodeGeneratorTest {

    private FlatCodeGenerator gen() { return new FlatCodeGenerator(); }

    // ─── toNativeType ───

    @Test
    public void testToNativeTypeBasic() {
        FlatCodeGenerator g = gen();
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
        FlatCodeGenerator g = gen();
        assertEquals("INT", g.toNativeType("PLC_INT_Value"));
        assertEquals("REAL", g.toNativeType("PLC_Real_Value"));
        assertEquals("BOOL", g.toNativeType("PLC_Bool_Value"));
        assertEquals("STRING", g.toNativeType("PLC_String_Value"));
    }

    @Test
    public void testToNativeTypeUnknown() {
        FlatCodeGenerator g = gen();
        assertEquals("MyCustomType", g.toNativeType("MyCustomType"));
    }

    @Test
    public void testToNativeTypeNull() {
        FlatCodeGenerator g = gen();
        assertNull(g.toNativeType(null));
    }

    @Test
    public void testToNativeTypeStruct() {
        FlatCodeGenerator g = gen();
        CodeGenerator.StructLayout layout = new CodeGenerator.StructLayout(
            "MyStruct",
            Arrays.asList(new CodeGenerator.StructField("X", "INT", 0)),
            2
        );
        g.registerStructType("MyStruct", "PLC_Struct_Value<123>", layout);
        assertEquals("MyStruct", g.toNativeType("PLC_Struct_Value<123>"));
    }

    // ─── getTypeSize ───

    @Test
    public void testGetTypeSize() {
        FlatCodeGenerator g = gen();
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
        FlatCodeGenerator g = gen();
        CodeGenerator.StructLayout layout = new CodeGenerator.StructLayout(
            "Point", Arrays.asList(
                new CodeGenerator.StructField("X", "INT", 0),
                new CodeGenerator.StructField("Y", "INT", 2)
            ), 4
        );
        g.registerStructType("Point", "PLC_Struct_Value<99>", layout);
        assertEquals(4, g.getTypeSize("Point"));
    }

    @Test
    public void testGetTypeSizeUnknown() {
        FlatCodeGenerator g = gen();
        assertEquals(4, g.getTypeSize("UnknownType"));
    }

    @Test
    public void testGetTypeSizeNull() {
        FlatCodeGenerator g = gen();
        assertEquals(4, g.getTypeSize(null));
    }

    // ─── emitHeader / emitFooter ───

    @Test
    public void testEmitHeader() {
        FlatCodeGenerator g = gen();
        String header = g.emitHeader();
        assertTrue(header.contains("rt_plc.h"));
        assertTrue(header.contains("rt_runtime.h"));
        assertTrue(header.contains("using namespace rt_plc"));
    }

    @Test
    public void testEmitFooter() {
        assertEquals("", gen().emitFooter());
    }

    // ─── emitVarDecl + offset allocation ───

    @Test
    public void testEmitVarDeclAllocatesOffset() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("A", "INT", null);
        Map<String, Integer> offsets = g.getOffsetMap();
        assertEquals(Integer.valueOf(0), offsets.get("A"));
        assertEquals("INT", g.getTypeMap().get("A"));
    }

    @Test
    public void testEmitVarDeclWithInitValue() {
        FlatCodeGenerator g = gen();
        g.registerVariable("A", "100");
        String result = g.emitVarDecl("A", "INT", "42");
        assertTrue(result.contains("gvl.write<INT>"));
        assertEquals(Integer.valueOf(0), g.getOffsetMap().get("A"));
    }

    @Test
    public void testEmitVarDeclAlignment() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("A", "SINT", null);     // offset 0, size 1, takes 0..0
        g.emitVarDecl("B", "INT", null);      // align to 2 → offset 2, takes 2..3
        assertEquals(Integer.valueOf(0), g.getOffsetMap().get("A"));
        assertEquals(Integer.valueOf(2), g.getOffsetMap().get("B"));
    }

    @Test
    public void testEmitVarDeclRealAlignment() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("A", "SINT", null);     // offset 0, size 1
        g.emitVarDecl("B", "REAL", null);     // align to 4 → offset 4
        assertEquals(Integer.valueOf(0), g.getOffsetMap().get("A"));
        assertEquals(Integer.valueOf(4), g.getOffsetMap().get("B"));
    }

    @Test
    public void testEmitVarDeclMultipleTypes() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("A", "INT", null);      // 0-1
        g.emitVarDecl("B", "DINT", null);     // 4-7 (aligned to 4)
        g.emitVarDecl("C", "REAL", null);     // 8-11
        assertEquals(Integer.valueOf(0), g.getOffsetMap().get("A"));
        assertEquals(Integer.valueOf(4), g.getOffsetMap().get("B"));
        assertEquals(Integer.valueOf(8), g.getOffsetMap().get("C"));
    }

    @Test
    public void testEmitVarDeclArray() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("ARR", "ARRAY[0..4] OF INT", null);
        assertEquals("ARRAY[5] OF INT", g.getTypeMap().get("ARR"));
        assertEquals(Integer.valueOf(0), g.getOffsetMap().get("ARR"));
    }

    // ─── emitAssign ───

    @Test
    public void testEmitAssign() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("A", "INT", null);
        g.registerVariable("A", "100");
        String code = g.emitAssign("A", "42");
        assertTrue(code.contains("gvl.write<INT>(0, 42)"));
        assertFalse(code.contains("gvl.read"));
    }

    @Test
    public void testEmitAssignWithExpression() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("A", "INT", null);
        g.emitVarDecl("B", "INT", null);
        g.registerVariable("A", "100");
        g.registerVariable("B", "101");
        String code = g.emitAssign("C", "A+B");
        assertTrue(code.contains("gvl.read<INT>(0)"));
        assertTrue(code.contains("gvl.read<INT>(2)"));
    }

    // ─── emitFuncReturnAssign ───

    @Test
    public void testEmitFuncReturnAssign() {
        FlatCodeGenerator g = gen();
        String code = g.emitFuncReturnAssign("42");
        assertTrue(code.contains("return 42"));
    }

    // ─── Control flow ───

    @Test
    public void testEmitIfBegin() {
        FlatCodeGenerator g = gen();
        String code = g.emitIfBegin("true");
        assertTrue(code.contains("if(true)"));
    }

    @Test
    public void testEmitElseIf() {
        FlatCodeGenerator g = gen();
        String code = g.emitElseIf("false");
        assertTrue(code.contains("else if(false)"));
    }

    @Test
    public void testEmitElseAndIfEnd() {
        assertEquals("\n\t\t}else{", gen().emitElse());
        assertEquals("\n\t\t}", gen().emitIfEnd());
    }

    @Test
    public void testEmitWhileBeginEnd() {
        FlatCodeGenerator g = gen();
        assertTrue(gen().emitWhileBegin("true").contains("while(true)"));
        assertEquals("\n\t\t}", gen().emitWhileEnd());
    }

    @Test
    public void testEmitRepeatBeginEnd() {
        FlatCodeGenerator g = gen();
        assertTrue(gen().emitRepeatBegin().contains("do{"));
        assertTrue(gen().emitRepeatEnd("true").contains("while(!(true))"));
    }

    @Test
    public void testEmitCaseBeginEnd() {
        FlatCodeGenerator g = gen();
        assertTrue(gen().emitCaseBegin("X").contains("switch(X)"));
        assertTrue(gen().emitCaseOption("1").contains("case 1"));
        assertTrue(gen().emitCaseDefault().contains("default"));
        assertEquals("\n\t\t}", gen().emitCaseEnd());
    }

    // ─── FOR loop ───

    @Test
    public void testEmitForBeginNoGvl() {
        FlatCodeGenerator g = gen();
        String code = g.emitForBegin("I", "1", "10", "1");
        assertTrue(code.contains("for( I = 1;"));
        assertTrue(code.contains("I <= 10;"));
        assertTrue(code.contains("I = I + 1)"));
    }

    @Test
    public void testEmitForBeginWithGvl() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("A", "INT", null);
        String code = g.emitForBegin("A", "1", "10", "1");
        assertTrue(code.contains("INT A = 1"));
        assertTrue(code.contains("for( ; A <= 10;"));
        assertTrue(code.contains("A = A + 1)"));
    }

    @Test
    public void testEmitForEndWritesBack() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("A", "INT", null);
        g.emitForBegin("A", "1", "10", "1");
        String code = g.emitForEnd();
        assertTrue(code.contains("gvl.write<INT>(0, A)"));
    }

    @Test
    public void testEmitForEndNoGvl() {
        FlatCodeGenerator g = gen();
        g.emitForBegin("I", "1", "10", "1");
        String code = g.emitForEnd();
        assertEquals("\n\t\t}", code);
    }

    // ─── Program / Function ───

    @Test
    public void testEmitFuncDeclBeginEnd() {
        FlatCodeGenerator g = gen();
        String code = g.emitFuncDeclBegin("MYFUNC", "INT", "INT X");
        assertTrue(code.contains("INT MYFUNC(INT X)"));
        assertEquals("\n}", g.emitFuncDeclEnd());
    }

    @Test
    public void testEmitFuncCall() {
        FlatCodeGenerator g = gen();
        String code = g.emitFuncCall("ADD", Arrays.asList("1", "2"));
        assertEquals("ADD(1, 2)", code);
    }

    @Test
    public void testEmitProgDeclBeginEnd() {
        FlatCodeGenerator g = gen();
        assertTrue(gen().emitProgDeclBegin("MAIN").contains("void PROGRAM_MAIN"));
        assertEquals("\n}", gen().emitProgDeclEnd());
    }

    @Test
    public void testEmitProgLifecycle() {
        FlatCodeGenerator g = gen();
        assertTrue(gen().emitProgInitBegin("MAIN").contains("PROGRAM_MAIN_init"));
        assertTrue(gen().emitProgCyclicBegin("MAIN").contains("PROGRAM_MAIN_cyclic"));
        assertTrue(gen().emitProgPreBegin("MAIN").contains("PROGRAM_MAIN_pre"));
        assertTrue(gen().emitProgPostBegin("MAIN").contains("PROGRAM_MAIN_post"));
        assertEquals("\n}", gen().emitProgFuncEnd());
    }

    // ─── POU Registration ───

    @Test
    public void testAddProgramName() {
        FlatCodeGenerator g = gen();
        g.addProgramName("MAIN");
        g.addProgramName("SUB");
        assertEquals(2, g.getProgramNames().size());
        assertEquals("MAIN", g.getProgramNames().get(0));
        assertEquals("SUB", g.getProgramNames().get(1));
    }

    @Test(expected = RuntimeException.class)
    public void testAddProgramNameDuplicate() {
        FlatCodeGenerator g = gen();
        g.addProgramName("MAIN");
        g.addProgramName("MAIN");
    }

    @Test
    public void testSetFileId() {
        FlatCodeGenerator g = gen();
        g.setFileId("test");
        assertEquals("test", g.getFileId());
    }

    @Test
    public void testGetFileIdDefault() {
        assertEquals("unnamed", gen().getFileId());
    }

    @Test
    public void testEmitPOURegistration() {
        FlatCodeGenerator g = gen();
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

    // ─── PRINT ───

    @Test
    public void testEmitPrintElementString() {
        assertEquals("\n\t\tprintf(\"hello\");", gen().emitPrintElement("\"hello\"", true));
    }

    @Test
    public void testEmitPrintElementInt() {
        FlatCodeGenerator g = gen();
        String code = g.emitPrintElement("42", false);
        assertTrue(code.contains("printf(\"%d\""));
        assertTrue(code.contains("(42)"));
    }

    // ─── ASSERT ───

    @Test
    public void testEmitAssert() {
        FlatCodeGenerator g = gen();
        String code = g.emitAssert("true", "1=1", 10);
        assertTrue(code.contains("_st_assert"));
        assertTrue(code.contains("[PASS]"));
        assertTrue(code.contains("[FAIL]"));
        assertTrue(code.contains("line 10"));
    }

    // ─── translateExpr: literal ───

    @Test
    public void testTranslateExprLiteralInt() {
        FlatCodeGenerator g = gen();
        assertEquals("(2)", g.translateExpr("(*(new INT(2)))"));
    }

    @Test
    public void testTranslateExprLiteralBool() {
        FlatCodeGenerator g = gen();
        assertEquals("(true)", g.translateExpr("(*(new BOOL(TRUE)))"));
        assertEquals("(false)", g.translateExpr("(*(new BOOL(FALSE)))"));
    }

    @Test
    public void testTranslateExprLiteralReal() {
        FlatCodeGenerator g = gen();
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
        FlatCodeGenerator g = gen();
        g.emitVarDecl("A", "INT", null);
        g.registerVariable("A", "123");
        String result = g.translateExpr("(*::PLC::RFM->getSymbolByID<INT*>(123))");
        assertEquals("gvl.read<INT>(0)", result);
    }

    @Test
    public void testTranslateExprRfmVarSimple() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("B", "REAL", null);
        g.registerVariable("B", "456");
        String result = g.translateExpr("::PLC::RFM->getSymbolByID<REAL*>(456)");
        assertEquals("gvl.read<REAL>(0)", result);
    }

    // ─── translateExpr: RFM function call ───

    @Test
    public void testTranslateExprRfmFuncCall() {
        FlatCodeGenerator g = gen();
        String result = g.translateExpr("*::PLC::RFM->getSymbolByID<MYFUNC*>(999)->callFunc(1, 2)");
        assertEquals("MYFUNC(1, 2)", result);
    }

    // ─── translateExpr: bare variable → GVL read ───

    @Test
    public void testTranslateExprBareVariable() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("A", "INT", null);
        String result = g.translateExpr("A + B");
        assertTrue(result.contains("gvl.read<INT>(0)"));
        assertTrue(result.contains("+"));
    }

    @Test
    public void testTranslateExprBareVariableMultiple() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("X", "INT", null);
        g.emitVarDecl("Y", "INT", null);
        String result = g.translateExpr("X * Y");
        assertTrue(result.contains("gvl.read<INT>(0)"));
        assertTrue(result.contains("gvl.read<INT>(2)"));
    }

    // ─── translateExpr: array ───

    @Test
    public void testTranslateExprArrayAccess() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("ARR", "ARRAY[0..4] OF INT", null);
        String result = g.translateExpr("ARR[I]");
        assertTrue(result.contains("gvl.safeArrayAt<INT>"));
        assertTrue(result.contains("0, I, 5"));
    }

    // ─── Struct field offset ───

    @Test
    public void testGetStructFieldOffset() {
        FlatCodeGenerator g = gen();
        CodeGenerator.StructLayout layout = new CodeGenerator.StructLayout(
            "Point", Arrays.asList(
                new CodeGenerator.StructField("X", "INT", 0),
                new CodeGenerator.StructField("Y", "INT", 2)
            ), 4
        );
        g.registerStructType("Point", "PLC_Struct_Value<99>", layout);
        assertEquals(Integer.valueOf(0), g.getStructFieldOffset("Point", "X"));
        assertEquals(Integer.valueOf(2), g.getStructFieldOffset("Point", "Y"));
    }

    @Test
    public void testGetStructFieldOffsetUnknown() {
        FlatCodeGenerator g = gen();
        assertNull(g.getStructFieldOffset("Unknown", "X"));
        assertNull(g.getStructFieldOffset("Point", "Z"));
    }

    // ─── getOffsetDefinitions ───

    @Test
    public void testGetOffsetDefinitions() {
        FlatCodeGenerator g = gen();
        g.emitVarDecl("A", "INT", null);
        g.emitVarDecl("B", "REAL", null);
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

    // ─── write ───

    @Test
    public void testWrite() {
        assertEquals("hello", gen().write("hello"));
    }
}