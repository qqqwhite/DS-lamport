public class HttpBuilder2 {

    private String request = "";

    public HttpBuilder2 buildRequestLine(String type, String url, String version){
        request += type + " " + url + " " + version + "\r\n";
        return this;
    }

    public HttpBuilder2 buildRequestHeader(String key, String value){
        request += key + ": " + value + "\r\n";
        return this;
    }
    public HttpBuilder2 buildRequestBody(String data){
        request += HttpBuilder.headerContentLength + ": " + data.length() + "\r\n\r\n";
        request += data;
        return this;
    }
    public HttpBuilder2 buildRequestBody(){
        request += HttpBuilder.headerContentLength + ": " + 0 + "\r\n\r\n";
        return this;
    }
    public String builder() {
        return request;
    }
}
