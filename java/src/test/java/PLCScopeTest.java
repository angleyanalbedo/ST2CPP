import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCImportScopeTypeDeclType;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PLCScopeTest {

    @Before
    public void setUp() {
        PLCScopeStack.stackInit();
    }

    private PLCImportScopeTypeDeclType makeSymbol(String name) {
        PLCImportScopeTypeDeclType sym = new PLCImportScopeTypeDeclType();
        sym.setName(name);
        sym.setRuntimeName(name);
        sym.setRuntimeTypeName(name);
        sym.setSort(PLCModifierEnum.Sort.FC_DECL);
        sym.setLocalScope(PLCScopeStack.currentScope);
        return sym;
    }

    @Test
    public void testGlobalScopeExists() {
        assertNotNull(PLCScopeStack.globalScope);
        assertEquals(0, PLCScopeStack.globalScope.getScopeDepth());
    }

    @Test
    public void testPushCreatesNewScope() {
        PLCImportScopeTypeDeclType sym = makeSymbol("TEST1");
        PLCScopeStack.push(sym);
        assertEquals("TEST1", PLCScopeStack.currentScope.getDeclSymbol().getName());
        assertEquals(1, PLCScopeStack.currentScope.getScopeDepth());
        PLCScopeStack.pop();
    }

    @Test
    public void testPushPopScopeDepth() {
        PLCImportScopeTypeDeclType outer = makeSymbol("OUTER");
        PLCScopeStack.push(outer);
        assertEquals(1, PLCScopeStack.currentScope.getScopeDepth());

        PLCImportScopeTypeDeclType inner = makeSymbol("INNER");
        PLCScopeStack.push(inner);
        assertEquals(2, PLCScopeStack.currentScope.getScopeDepth());

        PLCScopeStack.pop();
        assertEquals(1, PLCScopeStack.currentScope.getScopeDepth());

        PLCScopeStack.pop();
        assertEquals(0, PLCScopeStack.currentScope.getScopeDepth());
    }

    @Test
    public void testShallowFindSymbol() {
        PLCImportScopeTypeDeclType sym = makeSymbol("SCOPE_A");
        PLCScopeStack.push(sym);

        PLCSymbol var = new PLCSymbol();
        var.name = "VAR_A";
        var.symbolId = 100;
        PLCScopeStack.currentSymbolTable.addSymbol(var);

        PLCSymbol found = PLCScopeStack.currentScope.shallowFindSymbol("VAR_A");
        assertNotNull(found);
        assertEquals("VAR_A", found.name);

        assertNull(PLCScopeStack.currentScope.shallowFindSymbol("VAR_B"));

        PLCScopeStack.pop();
    }

    @Test
    public void testDeepFindSymbolFromParent() {
        PLCImportScopeTypeDeclType outer = makeSymbol("OUTER");
        PLCScopeStack.push(outer);
        PLCSymbol outerVar = new PLCSymbol();
        outerVar.name = "OUTER_VAR";
        outerVar.symbolId = 1;
        PLCScopeStack.currentSymbolTable.addSymbol(outerVar);

        PLCImportScopeTypeDeclType inner = makeSymbol("INNER");
        PLCScopeStack.push(inner);

        PLCSymbol found = PLCScopeStack.currentScope.deepFindSymbol("OUTER_VAR");
        assertNotNull(found);
        assertEquals("OUTER_VAR", found.name);

        PLCScopeStack.pop();
        PLCScopeStack.pop();
    }

    @Test
    public void testDeepFindSymbolShadowed() {
        PLCImportScopeTypeDeclType outer = makeSymbol("OUTER");
        PLCScopeStack.push(outer);
        PLCSymbol outerVar = new PLCSymbol();
        outerVar.name = "X";
        outerVar.symbolId = 1;
        PLCScopeStack.currentSymbolTable.addSymbol(outerVar);

        PLCImportScopeTypeDeclType inner = makeSymbol("INNER");
        PLCScopeStack.push(inner);
        PLCSymbol innerVar = new PLCSymbol();
        innerVar.name = "X";
        innerVar.symbolId = 2;
        PLCScopeStack.currentSymbolTable.addSymbol(innerVar);

        PLCSymbol found = PLCScopeStack.currentScope.deepFindSymbol("X");
        assertNotNull(found);
        assertEquals(2, found.symbolId);

        PLCScopeStack.pop();
        PLCScopeStack.pop();
    }

    @Test
    public void testScopeLocation() {
        PLCImportScopeTypeDeclType sym = makeSymbol("FUNC_A");
        PLCScopeStack.push(sym);
        assertTrue(PLCScopeStack.currentScope.getScopeLocation().contains("FUNC_A"));
        PLCScopeStack.pop();
    }

    @Test
    public void testScopeIdUnique() {
        PLCImportScopeTypeDeclType s1 = makeSymbol("S1");
        PLCScopeStack.push(s1);
        int id1 = PLCScopeStack.currentScope.getScopeID();

        PLCImportScopeTypeDeclType s2 = makeSymbol("S2");
        PLCScopeStack.push(s2);
        int id2 = PLCScopeStack.currentScope.getScopeID();

        assertNotEquals(id1, id2);

        PLCScopeStack.pop();
        PLCScopeStack.pop();
    }

    @Test
    public void testParentScope() {
        PLCScope global = PLCScopeStack.globalScope;
        assertNull(global.getParentScope());

        PLCImportScopeTypeDeclType sym = makeSymbol("CHILD");
        PLCScopeStack.push(sym);
        assertEquals(global, PLCScopeStack.currentScope.getParentScope());
        PLCScopeStack.pop();
    }

    @Test
    public void testChildScopeList() {
        PLCScope global = PLCScopeStack.globalScope;
        int initialChildren = global.childScopeList.size();

        PLCImportScopeTypeDeclType sym = makeSymbol("CHILD1");
        PLCScopeStack.push(sym);
        int afterPush = global.childScopeList.size();
        assertEquals(initialChildren + 1, afterPush);
        PLCScopeStack.pop();
        assertTrue(global.childScopeList.size() >= initialChildren);
    }

    @Test
    public void testDeepFindAllSymbols() {
        PLCImportScopeTypeDeclType outer = makeSymbol("OUTER");
        PLCScopeStack.push(outer);
        PLCSymbol v1 = new PLCSymbol(); v1.name = "X"; v1.symbolId = 1;
        PLCScopeStack.currentSymbolTable.addSymbol(v1);

        PLCImportScopeTypeDeclType inner = makeSymbol("INNER");
        PLCScopeStack.push(inner);
        PLCSymbol v2 = new PLCSymbol(); v2.name = "X"; v2.symbolId = 2;
        PLCScopeStack.currentSymbolTable.addSymbol(v2);

        assertEquals(2, PLCScopeStack.currentScope.deepFindAllSymbols("X").size());

        PLCScopeStack.pop();
        PLCScopeStack.pop();
    }

    @Test
    public void testCurrentSymbolTable() {
        assertNotNull(PLCScopeStack.currentSymbolTable);
        PLCImportScopeTypeDeclType sym = makeSymbol("TEST");
        PLCScopeStack.push(sym);
        assertNotNull(PLCScopeStack.currentSymbolTable);
        assertNotEquals(PLCScopeStack.globalSymbolTable, PLCScopeStack.currentSymbolTable);
        PLCScopeStack.pop();
        assertEquals(PLCScopeStack.globalSymbolTable, PLCScopeStack.currentSymbolTable);
    }
}