package PLCSymbolAndScope;

//生成唯一ID标记符号、符号表、作用域对象
public class IDGenerator {

    protected IDGenerator(){}

    static private IDGenerator idGenerator;

    static {
        idGenerator = new IDGenerator();
    }

    static public IDGenerator getIDGenerator(){
        return idGenerator;
    }

    //内置类型id，待补充,符号id和类型id需要随之修改
    final static public int SINTID = 0;
    final static public int INTID = 1;
    final static public int DINTID = 2;
    final static public int LINTID = 3;
    final static public int SSTRING = 4;
    final static public int BOOL = 5;
    final static public int REAL = 6;
    final static public int TIME = 7;
    final static public int BITSTR = 8;

    static private final int count = 100;
    //符号id
    static int symbolId = count;

    //类型id
    static int typeId = count;

    //符号表id
    static int tableId = 0;

    //作用域id
    static int scopeId = 0;

    static int tempVarId = 0;


    //输出ID

    public int newSymbolId(){
        return symbolId++;
    }

    public int newTableId(){
        return tableId++;
    }

    public int newScopeId(){
        return scopeId++;
    }

    public int newTypeId(){
        return typeId++;
    }

    public int newTempId(){
        return tempVarId++;
    }


}
