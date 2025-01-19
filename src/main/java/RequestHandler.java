import java.util.HashMap;
import java.util.Map;

public class RequestHandler extends HttpHandler{

    private String requestType;

    private String url;

    private final Map<String,String> params = new HashMap<>();

    private String version;

    public RequestHandler(String request){
        String[] lines = request.split("\r\n\r\n")[0].split("\r\n");
        handlerLine(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            handlerHeader(lines[i]);
        }
        if (getHeaderOrDefault(HttpBuilder.headerContentLength, "0").equals("0")){
            setBody(null);
        }else {
            setBody(getRequestBody(request));
        }
    }

    private void handlerLine(String line){
        String[] parts = line.split(" ");
        requestType = parts[0];
        handlerUrl(parts[1]);
        version = parts[2];
    }

    private void handlerUrl(String url){
        String[] parts = url.split("\\?");
        this.url = parts[0];
        if (parts.length > 1){
            String[] params = parts[1].split("&");
            for (String param : params){
                String[] keyValue = param.split("=");
                this.params.put(keyValue[0],keyValue[1]);
            }
        }
    }

    public String getParam(String key){
        return params.get(key);
    }

    private void handlerHeader(String line){
        String[] parts = line.split(": ");
        if (parts.length == 2) {

            setHeaderInfo(parts[0], parts[1]);
        }
    }

    private String getRequestBody(String request){
        int beginIndex = request.indexOf("\r\n\r\n")+4;
        int length = Integer.parseInt(getHeaderInfo(HttpBuilder.headerContentLength));
        return request.substring(beginIndex, beginIndex+length);
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
