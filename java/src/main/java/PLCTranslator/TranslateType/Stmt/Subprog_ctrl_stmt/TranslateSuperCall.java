package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCBaseClassDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateSuperCall {
    public String translateNode(PLCSTPARSERParser.SuperCallContext ctx, PLCTranslatorNew translatorNew){
        String methodName = ctx.derived_func_name().getText();

        // 查找父类名称
        String parentClassName = resolveParentClassName(translatorNew.currentClassName);

        if (parentClassName != null) {
            // 生成 this->ParentClass::MethodName() 调用（C++ 调用父类成员方法的正确语法）
            return "\n\t\tthis->" + parentClassName + "::" + methodName + "();";
        }

        // 回退：无法解析父类时输出注释
        return "\n\t\t// SUPER::" + methodName + "(this); /* FIXME: resolve parent class */";
    }

    private String resolveParentClassName(String currentClassName) {
        if (currentClassName == null) return null;

        // 从全局类型表中查找当前类
        for (PLCTypeDeclSymbol typeDecl : PLCTotalSymbolTable.totalTypeMap.values()) {
            if (typeDecl.getName().equals(currentClassName) && typeDecl instanceof PLCBaseClassDeclSymbol classDecl) {
                PLCBaseClassDeclSymbol baseClass = classDecl.getBaseClass();
                if (baseClass != null) {
                    return baseClass.getName();
                }
            }
        }
        return null;
    }
}
