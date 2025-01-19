import java.util.HashMap;
import java.util.Map;

public class ResponseHandler extends HttpHandler{
    private String msg;

    private String code;

    private String version;

    public ResponseHandler(String response){
        String[] lines = response.split("\r\n\r\n")[0].split("\r\n");
        HandlerLine(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            HandlerHeader(lines[i]);
        }
        if (getHeaderOrDefault(HttpBuilder.headerContentLength, "0").equals("0")){
            setBody(null);
        }else {
            setBody(getResponseBody(response));
        }
    }

    private void HandlerLine(String line){
        String[] parts = line.split(" ");
        version = parts[0];
        code = parts[1];
        msg = parts[2];
    }

    private void HandlerHeader(String line){
        String[] parts = line.split(": ");
        if (parts.length == 2) {
            setHeaderInfo(parts[0], parts[1]);
        }
    }

    private String getResponseBody(String response){
        int beginIndex = response.indexOf("\r\n\r\n")+4;
        int length = Integer.parseInt(getHeaderInfo(HttpBuilder.headerContentLength));
        return response.substring(beginIndex, beginIndex+length);
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
