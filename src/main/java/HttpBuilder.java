import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;


// TODO: cancel it
public class HttpBuilder {

    static String GET = "GET";
    static String PUT = "PUT";

    static String version = "HTTP/1.1";

    static String headerUserAgent = "User-Agent";

    static String headerContentType = "Content-Type";

    static String headerContentLength = "Content-Length";

    static String headerJsonType= "application/json";

    static String headerTextType = "text/plain";

    static String paramTimestamp = "timestamp";

    public static String buildGET(String url) {
        StringBuilder result = new StringBuilder();
        result.append("GET "+url+" HTTP/1.1");
        result.append("\r\n");
        result.append("User-Agent: ATOMClient/1/0");
        result.append("\r\n");
        result.append("Content-Type: application/json");
        result.append("\r\n");
        result.append("Content-Length: 0");
        result.append("\r\n\r\n");
        return result.toString();
    }

    public static HttpBuilder2 builder(){
        return new HttpBuilder2();
    }

    public static String buildPUT(String url, String data, String serverName) {
        StringBuilder result = new StringBuilder();
        result.append("PUT "+url+" HTTP/1.1");
        result.append("\r\n");
        result.append("User-Agent: "+serverName);
        result.append("\r\n");
        result.append("Content-Type: application/json");
        result.append("\r\n");
        result.append("Content-Length: "+data.length());
        result.append("\r\n\r\n");
        result.append(data);
        return result.toString();
    }

    public static String buildResponse(String code, String msg, String data) {
        StringBuilder result = new StringBuilder();
        result.append("HTTP/1.1 "+code+" "+msg);
        result.append("\r\n");
        result.append("Content-Type: application/json");
        result.append("\r\n");
        result.append("Content-Length: "+data.length());
        result.append("\r\n\r\n");
        result.append(data);
        return result.toString();
    }

    public static String buildResponse(String code, String msg) {
        StringBuilder result = new StringBuilder();
        result.append("HTTP/1.1 "+code+" "+msg);
        result.append("\r\n");
        result.append("Content-Type: application/json");
        result.append("\r\n");
        result.append("Content-Length: 0");
        result.append("\r\n\r\n");
        return result.toString();
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        return baos.toByteArray();
    }

    public static String handleGET(String request) {
        return request.split(" ")[1];
    }

    public static String getRequestBody(String request) throws IOException {
        int beginIndex = request.indexOf("\r\n\r\n")+4;
        int length = Integer.parseInt(getTargetLine("Content-Length", request).split(": ")[1]);
        return request.substring(beginIndex, beginIndex+length);
    }

    public static String handlePUTServerName(String request) {
        return getTargetLine("User-Agent", request).substring("User-Agent: ".length());
    }

    public static String getTargetLine(String type, String request){
        String headRequest = request.split("\r\n\r\n")[0];
        for (String line: headRequest.split("\r\n")){
            if (line.startsWith(type)){
                return line;
            }
        }
        return "";
    }

    public static String buildUrl(String content, Map<String,String> params){
        StringBuilder url = new StringBuilder();
        url.append(content);
        int count = 0;
        for (Map.Entry<String,String> entry : params.entrySet()){
            url.append("?").append(entry.getKey()).append("=").append(entry.getValue());
            count++;
            if (count < params.size()){
                url.append("&");
            }
        }
        return url.toString();
    }

}
