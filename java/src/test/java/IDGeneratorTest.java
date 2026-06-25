import PLCSymbolAndScope.IDGenerator;
import org.junit.Test;
import static org.junit.Assert.*;

public class IDGeneratorTest {

    @Test
    public void testSingleton() {
        IDGenerator g1 = IDGenerator.getIDGenerator();
        IDGenerator g2 = IDGenerator.getIDGenerator();
        assertSame(g1, g2);
    }

    @Test
    public void testNewSymbolIdIncrements() {
        int first = IDGenerator.getIDGenerator().newSymbolId();
        int second = IDGenerator.getIDGenerator().newSymbolId();
        assertEquals(first + 1, second);
    }

    @Test
    public void testNewTableIdIncrements() {
        int first = IDGenerator.getIDGenerator().newTableId();
        int second = IDGenerator.getIDGenerator().newTableId();
        assertEquals(first + 1, second);
    }

    @Test
    public void testNewScopeIdIncrements() {
        int first = IDGenerator.getIDGenerator().newScopeId();
        int second = IDGenerator.getIDGenerator().newScopeId();
        assertEquals(first + 1, second);
    }

    @Test
    public void testNewTypeIdIncrements() {
        int first = IDGenerator.getIDGenerator().newTypeId();
        int second = IDGenerator.getIDGenerator().newTypeId();
        assertEquals(first + 1, second);
    }

    @Test
    public void testNewTempIdIncrements() {
        int first = IDGenerator.getIDGenerator().newTempId();
        int second = IDGenerator.getIDGenerator().newTempId();
        assertEquals(first + 1, second);
    }

    @Test
    public void testBuiltInTypeIds() {
        assertEquals(0, IDGenerator.SINTID);
        assertEquals(1, IDGenerator.INTID);
        assertEquals(2, IDGenerator.DINTID);
        assertEquals(3, IDGenerator.LINTID);
        assertEquals(4, IDGenerator.SSTRING);
        assertEquals(5, IDGenerator.BOOL);
        assertEquals(6, IDGenerator.REAL);
        assertEquals(7, IDGenerator.TIME);
        assertEquals(8, IDGenerator.BITSTR);
    }

    @Test
    public void testIdsAreDifferentTypes() {
        IDGenerator g = IDGenerator.getIDGenerator();
        int symId = g.newSymbolId();
        int typeId = g.newTypeId();
        int tableId = g.newTableId();
        int scopeId = g.newScopeId();
        assertNotEquals(symId, typeId);
        assertNotEquals(symId, tableId);
        assertNotEquals(symId, scopeId);
    }
}