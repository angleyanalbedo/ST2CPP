package PLCSymbolAndScope.PLCSymbols;

//symbol中所有的枚举变量都声明在这里
public final class PLCModifierEnum {
    //变量段类型
    public enum VarSections{
        VAR, VAR_INPUT, VAR_OUTPUT, VAR_IN_OUT, VAR_EXTERNAL, VAR_GLOBAL, VAR_ACCESS, VAR_TEMP, VAR_CONFIG
    }

    //访问修饰符
    public enum AccessModifier{
        PROTECTED, PUBLIC, PRIVATE, INTERNAL,NOT_DECLARED
    }

    //保留修饰符
    public enum RetainModifier {
        RETAIN, NON_RETAIN
    }

    //类和方法的修饰符
    public enum ClassModifier{
        FINAL, ABSTRACT, NONE
    }
    public enum Sort{
        //值类(只会在PLCVarymbolS中出现)
        //部分无意义
        ARRAY, CLASS, ENUM, FB, FC, INTERFACE, METHOD,
        NAMESPACE, PROGRAM, STRUCT, SUBRANGE, SUBTYPE, STRING, INT, REAL, CHAR, TIME, BITSTR, BOOL,
        REF,

        //类型类（ 只会在PLCTypeDeclSymbol中出现）
        ARRAY_DECL, CLASS_DECL, ENUM_DECL, FB_DECL, FC_DECL,INTERFACE_DECL, METHOD_DECL, NAMESPACE_DECL,
        PROGRAM_DECL, STRING_DECL, SUBRANGE_DECL, SUBTYPE_DECL, STRUCT_DECL, REF_DECL
    }
}
