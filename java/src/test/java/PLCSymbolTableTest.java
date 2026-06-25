import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import org.junit.Test;
import static org.junit.Assert.*;

public class PLCSymbolTableTest {

    @Test
    public void testConstructorAssignsId() {
        PLCSymbolTable table1 = new PLCSymbolTable();
        PLCSymbolTable table2 = new PLCSymbolTable();
        assertNotEquals(table1.getTableId(), table2.getTableId());
    }

    @Test
    public void testAddSymbolWithIdAndName() {
        PLCSymbolTable table = new PLCSymbolTable();
        PLCSymbol sym = new PLCSymbol();
        sym.name = "X";
        sym.symbolId = 100;
        table.addSymbol(sym, 100, "X");
        assertEquals(sym, table.findSymbol("X"));
        assertTrue(table.getSymbolIDHashMap().containsKey(100));
    }

    @Test
    public void testAddSymbolByObject() {
        PLCSymbolTable table = new PLCSymbolTable();
        PLCSymbol sym = new PLCSymbol();
        sym.name = "Y";
        sym.symbolId = 200;
        table.addSymbol(sym);
        assertEquals(sym, table.findSymbol("Y"));
        assertEquals(sym, table.getSymbolIDHashMap().get(200));
    }

    @Test
    public void testFindSymbolNotFound() {
        PLCSymbolTable table = new PLCSymbolTable();
        assertNull(table.findSymbol("DOES_NOT_EXIST"));
    }

    @Test
    public void testFindSymbolCaseSensitive() {
        PLCSymbolTable table = new PLCSymbolTable();
        PLCSymbol sym = new PLCSymbol();
        sym.name = "ABC";
        sym.symbolId = 1;
        table.addSymbol(sym);
        assertNotNull(table.findSymbol("ABC"));
        assertNull(table.findSymbol("abc"));
    }

    @Test
    public void testAddMultipleSymbols() {
        PLCSymbolTable table = new PLCSymbolTable();
        PLCSymbol a = new PLCSymbol(); a.name = "A"; a.symbolId = 1;
        PLCSymbol b = new PLCSymbol(); b.name = "B"; b.symbolId = 2;
        PLCSymbol c = new PLCSymbol(); c.name = "C"; c.symbolId = 3;
        table.addSymbol(a);
        table.addSymbol(b);
        table.addSymbol(c);
        assertEquals(3, table.getSymbolIDHashMap().size());
        assertEquals(a, table.findSymbol("A"));
        assertEquals(b, table.findSymbol("B"));
        assertEquals(c, table.findSymbol("C"));
    }

    @Test
    public void testOverwriteSymbol() {
        PLCSymbolTable table = new PLCSymbolTable();
        PLCSymbol first = new PLCSymbol(); first.name = "X"; first.symbolId = 1;
        PLCSymbol second = new PLCSymbol(); second.name = "X"; second.symbolId = 2;
        table.addSymbol(first);
        table.addSymbol(second);
        assertEquals(second, table.findSymbol("X"));
        assertTrue(table.getSymbolIDHashMap().containsKey(1));
        assertTrue(table.getSymbolIDHashMap().containsKey(2));
    }

    @Test
    public void testFindSameNamedSymbol() {
        PLCSymbolTable table = new PLCSymbolTable();
        PLCSymbol s1 = new PLCSymbol(); s1.name = "X"; s1.symbolId = 1;
        PLCSymbol s2 = new PLCSymbol(); s2.name = "X"; s2.symbolId = 2;
        table.addSymbol(s1, 1, "X");
        table.addSymbol(s2, 2, "X");
        assertEquals(2, table.findSameNamedSymbol("X").size());
        assertTrue(table.findSameNamedSymbol("X").contains(s1));
        assertTrue(table.findSameNamedSymbol("X").contains(s2));
    }

    @Test
    public void testFindSameNamedSymbolEmpty() {
        PLCSymbolTable table = new PLCSymbolTable();
        assertTrue(table.findSameNamedSymbol("NONE").isEmpty());
    }

    @Test
    public void testGetSymbolIDHashMap() {
        PLCSymbolTable table = new PLCSymbolTable();
        assertNotNull(table.getSymbolIDHashMap());
        assertTrue(table.getSymbolIDHashMap().isEmpty());
    }

    @Test
    public void testTableIdIsPositive() {
        PLCSymbolTable table = new PLCSymbolTable();
        assertTrue(table.getTableId() >= 0);
    }
}