package PLCSymbolAndScope.PLCSymbolTables;


import JSON.Format;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbols.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import staticCheckVisitor.GenerateArrayTypes;
import staticCheckVisitor.GenerateRefTypes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

//总符号表（依靠ID索引）
public final class PLCTotalSymbolTable {

    //类型id-类型声明符号表
    static public HashMap<Integer, PLCTypeDeclSymbol> totalTypeMap = new HashMap<>();

    static public PLCTypeDeclSymbol getTypeByTypeID(int typeID){
        return totalTypeMap.get(typeID);
    }
    //符号id-符号表
    static public HashMap<Integer, PLCSymbol> totalSymbolMap = new HashMap<>();

    //作用域ID-作用域表
    static public HashMap<Integer, PLCScope> totalScopeMap = new HashMap<>();

    //符号表ID--符号表
    static public HashMap<Integer, PLCSymbolTable> totalTableMap = new HashMap<>();

    //保存数组类型的表 键为类型的symbolid，值为类型的数组符号
    static public HashMap<Integer, ArrayList<PLCArrayDeclSymbol>> arraySymbolMap = new HashMap<>();

    //保存引用类型的表 键为类型的symbolid，值为类型的引用类型符号
    static public HashMap<Integer, PLCRefDeclSymbol> refSymbolMap = new HashMap<>();

    //将符号加入总表
    /**
     * 仅仅加入一个符号*/
    static public void addSymbol(PLCSymbol symbol){
        totalSymbolMap.put(symbol.getSymbolId(), symbol);
    }

    /**
     * 仅仅加入一个作用域*/
    static public void addScope(PLCScope scope){
        totalScopeMap.put(scope.getScopeID(), scope);
    }

    /**
     * 仅仅加入一个符号表*/
    static public void addTable(PLCSymbolTable table){
        totalTableMap.put(table.getTableId(), table);
    }

    /**
     * 添加一种类型（符号声明对应的类型，不添加符号）*/
    static public void addType(PLCTypeDeclSymbol declSymbol){
        totalTypeMap.put(declSymbol.typeId, declSymbol);
        new GenerateArrayTypes().generate(declSymbol);
        new GenerateRefTypes().generate(declSymbol);
    }

    /**
     * 集合添加: 一个作用域、符号、表*/
    static public void addBlock(PLCImportScopeTypeDeclType symbol){
        addSymbol(symbol);
        addScope(symbol.getImportScope());
        addTable(symbol.getImportSymbolTable());
    }

    static public void showTotalMap() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        PLCSymbolTable basicTypeTable = PLCScopeStack.basicTypeTable;
        JsonArray jsonArray = new JsonArray();
        HashMap<Integer, PLCSymbol> symbolIDHashMap = basicTypeTable.getSymbolIDHashMap();
        for (PLCSymbol value : symbolIDHashMap.values()) {
            jsonArray.add(value.toStringJson());
        }
        JsonObject basicTableJson = new JsonObject();
        basicTableJson.add("Basic Table",jsonArray);
        JsonArray entirety = new JsonArray();
        entirety.add(basicTableJson);
        //stringBuilder.append(Format.toPrettyString(basicTableJson))
                //.append("********************************")
        ;
        //Format.printlnJsonObjectString(basicTableJson);
        //System.out.println("********************************");
        for (PLCSymbolTable symbolTable : totalTableMap.values()) {
          //Format.printlnJsonObjectString(symbolTable.toJsonString());
           //System.out.println("********************************");
           //stringBuilder.append(Format.toPrettyString(symbolTable.toJsonString()))
            entirety.add(symbolTable.toJsonString())
                    //.append("********************************")
            ;
        }
        //Format.printlnJsonObjectString(entirety);
        stringBuilder.append(Format.toPrettyString(entirety));
        System.out.println(stringBuilder);
        OutputToFile(stringBuilder);
    }
    public static void OutputToFile(StringBuilder stringBuilder) throws IOException {
        String path = "src/main/resources/output/symbolTable.json";
        File file = new File(path);
        //如果文件不存在，则自动生成文件；
        if(!file.exists()){
            file.createNewFile();
        }

        //引入输出流
        OutputStream outPutStream;
        try{
            outPutStream = new FileOutputStream(file);



            String context = stringBuilder.toString();//将可变字符串变为固定长度的字符串，方便下面的转码；
            //byte[]  bytes = context.getBytes("UTF-8");//因为中文可能会乱码，这里使用了转码，转成UTF-8；
            outPutStream.write(context.getBytes());//开始写入内容到文件；
            outPutStream.close();//一定要关闭输出流；
        }catch(Exception e){
            e.printStackTrace();//获取异常
        }
    }

}
