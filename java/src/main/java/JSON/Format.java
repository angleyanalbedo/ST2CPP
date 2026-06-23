package JSON;
import com.google.gson.*;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class Format {
    public static String setString (String[] stringVar){
        for (String elemVar : stringVar){

        }
        return null;
    }
    public static void jsonToString(String jsonString) {
        String pretty = toPrettyFormat(jsonString);

        System.out.println(pretty);
    }

    /**
     * 格式化输出JSON字符串
     * @return 格式化后的JSON字符串
     */
    private static String toPrettyFormat(String json) {
        //JsonParser jsonParser = new JsonParser();
        //JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        JsonElement jsonParser = JsonParser.parseString(json);
        //JsonObject jsonObject = jsonParser.;
        //jsonObject.
        //Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //return gson.toJson(jsonParser);
        System.out.println(jsonParser);
        return null;
    }
    public static void testOfJson(int JsonNumber){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("int",JsonNumber);
        System.out.println("json"+jsonObject);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println("gson"+gson.toJson(jsonObject));
    }

    public static  String toPrettyString(JsonElement jsonElement){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(jsonElement);
    }
    public static void printlnJsonObjectString(JsonElement jsonObject){
        System.out.println(toPrettyString(jsonObject));
    }

}
