
ADD_LIBRARY(
        PLC_LIB STATIC

        # 运行基础
        PLC.h
        PLC_Object.h
        PLC_Object.cpp
        PLC_Value.h
        PLC_Value.cpp
        PLC_Type.h
        PLC_Type.cpp
        PLC_Type_Dict.h
        PLC_Type_Dict.cpp

        # 整形类
        PLC_INNER/PLC_Inner_Value.h
        PLC_INNER/PLC_Inner_Value.cpp
        PLC_INNER/PLC_Inner_Type.h
        PLC_INNER/PLC_INTEGER/PLC_SINT_Value.h
        PLC_INNER/PLC_INTEGER/PLC_SINT_Type.h
        PLC_INNER/PLC_INTEGER/PLC_Integer_Value.hpp
        PLC_INNER/PLC_INTEGER/PLC_Integer_Type.hpp
        PLC_INNER/PLC_INTEGER/PLC_INT_Value.h
        PLC_INNER/PLC_INTEGER/PLC_INT_Type.h

        # 字符串类
        PLC_INNER/PLC_STRING/PLC_BasicString_Value.hpp
        PLC_INNER/PLC_STRING/PLC_BasicString_Type.hpp
        PLC_INNER/PLC_STRING/PLC_String_Value.h
        PLC_INNER/PLC_STRING/PLC_String_Type.h
        PLC_INNER/PLC_STRING/PLC_WString_Value.h
        PLC_INNER/PLC_STRING/PLC_WString_Type.h

        # 结构体类
        PLC_STRUCT/PLC_Struct_Value.hpp
        PLC_STRUCT/PLC_Struct_Type.hpp
        PLC_STRUCT/PLC_Struct_FieldInfo.h
        PLC_STRUCT/PLC_Struct_FieldInfo.cpp

        # 枚举类
        PLC_ENUM/PLC_Enum_Value.hpp
        PLC_ENUM/PLC_Enum_Type.hpp

        # 数组类
        PLC_ARRAY/PLC_Array_Type.hpp
        PLC_ARRAY/PLC_Array_Value.hpp

        PLC_Function/PLC_Function.h
        PLC_Function/PLC_Translated_Function.h

        PLC_CLASS/PLC_Class.h
        PLC_CLASS/PLC_Translated_Class.h

#        PLC_ARRAY/PLC_Array_Value.hpp
#        PLC_ARRAY/PLC_Array_Type.hpp

        PLC_Exception/PLC_Exception.h
        PLC_Exception/PLC_Exception.cpp

        PLC_INNER/PLC_INTEGER/PLC_Real_Type.h
        PLC_INNER/PLC_INTEGER/PLC_Real_Value.h

        MemoryManager/MemoryManager.cpp
        MemoryManager/MemoryManager.h TCI/TCI.h

        PLC_SUBTYPE/PLC_Subtype_Type.hpp PLC_INTERFACE/PLC_INTERFACE.cpp
        PLC_INTERFACE/PLC_INTERFACE.h varMap/varMap.cpp varMap/varMap.h 
        PLC_Method/PLC_Method.cpp PLC_Method/PLC_Method.h 
        Debug.h)