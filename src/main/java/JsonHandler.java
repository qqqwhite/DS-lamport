import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class JsonHandler {

    private final static Gson gson = new Gson();

    public static String loadFile(String path) throws IOException {
        Path path1 = Paths.get(path);
        Map<String, Object> map = new HashMap<>();
        if (Files.exists(path1)){
            String fileString = new String(Files.readAllBytes(path1));
            for (String line : fileString.split("\n")) {
                String key = line.split(":")[0].trim();
                String value = line.split(":")[1].trim();
                map.put(key, value);
            }
        }
        return gson.toJson(map);
    }

    public static String transMap(Map<String, Object> map) {
        return gson.toJson(map);
    }

    public static JsonObject transStringToJson(String info) throws IOException {
        return JsonParser.parseString(info).getAsJsonObject();
    }

    public static boolean checkJson(String info){
        try{
            JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public static String jsonStringToString(String info) throws IOException {
        JsonObject jsonObject = transStringToJson(info);
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            builder.append(entry.getKey()).append(":").append(entry.getValue().getAsString());
            builder.append("\n");
        }
        return builder.toString();
    }

}
