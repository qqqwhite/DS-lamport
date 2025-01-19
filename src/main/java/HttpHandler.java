import java.util.HashMap;
import java.util.Map;

public abstract class HttpHandler {

    private String body;

    private Map<String, String> mapHttpHeader = new HashMap<>();

    public String getHeaderInfo(String key){
        return mapHttpHeader.get(key);
    }

    public String getHeaderOrDefault(String key, String defaultValue){
        return mapHttpHeader.get(key) == null ? defaultValue : mapHttpHeader.get(key);
    }

    public void setHeaderInfo(String key, String value){
        mapHttpHeader.put(key, value);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }



}
