import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class GETClient extends LamportServer{

    public GETClient(int port,String name, String aggrIp, int aggrPort) throws IOException {
        super(port, name, aggrIp, aggrPort);
    }

    public GETClient(int port,String name, String aggrIp, int aggrPort, boolean isTestingModel) throws IOException {
        super(port, name, aggrIp, aggrPort, isTestingModel);
    }

    @Override
    public void start() throws IOException {
        new Thread(){
            @Override
            public void run() {
                try {
                    server();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
        // get address from aggregation
        getAllAddr();
        // get lamport time
        getLamportTime();// 同步当前时钟
        if (config.isTestingModel){
            for (String typeServerName:ipPorts.keySet()){
                if (Util.getClassName(typeServerName).equals(ContentServer.class.getName())){
                    getLamportTime();
                    getWeatherInfo(Util.getServerName(typeServerName));
                }
            }
        }else {
            while (true) {
                getAllAddr();
                showOptionalContentServerName();
                System.out.println("Choosing one Content Server: ");
                Scanner scanner = new Scanner(System.in);
                String ContentServerName = scanner.nextLine();
                //TODO: check ContentServerName is valid
                getLamportTime();
                System.out.println("Weather Info: \n");
                getWeatherInfo(ContentServerName);
                System.out.println();
            }
        }
    }

    private void showOptionalContentServerName(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Optional Content Server Names: \n");
        for (Map.Entry<String, String> entry : ipPorts.entrySet()) {
            stringBuilder.append("Content server name: ");
            stringBuilder.append(Util.getServerName(entry.getKey()));
            stringBuilder.append(" ");
            stringBuilder.append(entry.getValue());
            stringBuilder.append("\n");
        }
        System.out.println(stringBuilder.toString());
    }

    public String getWeatherInfo(String ContentServerName) throws IOException {
        SocketChannel socketChannel1 = SocketChannel.open();
        socketChannel1.configureBlocking(true);
        socketChannel1.connect(new InetSocketAddress(String.valueOf(this.aggrIp), this.aggrPort));
        while(!socketChannel1.finishConnect()){
            Thread.yield();//fix
        }
        // TODO: send lamport
        // send msg
        Map<String, String> params = new HashMap<>();
        params.put(HttpBuilder.paramTimestamp, String.valueOf(time));
        String request= HttpBuilder.builder()
                .buildRequestLine(HttpBuilder.GET, HttpBuilder.buildUrl("/weather/"+ContentServerName, params), HttpBuilder.version)
                .buildRequestHeader(HttpBuilder.headerUserAgent, this.name)
                .buildRequestHeader(HttpBuilder.headerContentType, HttpBuilder.headerJsonType)
                .buildRequestBody()
                .builder();
        // get lamport time
        // TODO: handle weather info
        sendMessage(request, socketChannel1);
        String response = handleResponse(socketChannel1);
        ResponseHandler responseHandler = new ResponseHandler(response);
        if (config.isTestingModel) System.out.println(name+" received response on Opt: "+response);
        if (responseHandler.getCode().equals("200")){
            String weatherInfo = responseHandler.getBody();
            System.out.println(JsonHandler.jsonStringToString(weatherInfo));
        }else if (responseHandler.getCode().equals("204")){
            System.err.println(responseHandler.getMsg());
        }
        return responseHandler.getBody();
    }

    @Override
    protected void handleRequest(SocketChannel socketChannel, String request) throws IOException {
        // 接收请求
        // 根据type分类，然后根据url拿到唯一对应的处理方法，进行处理，并发送response
        RequestHandler requestHandler = new RequestHandler(request);
        if (requestHandler.getRequestType().equals(HttpBuilder.GET)){
            if (requestHandler.getUrl().equals("/lamport")){
                handleLamportRequest(socketChannel, requestHandler);
            }
        }else if (requestHandler.getRequestType().equals(HttpBuilder.PUT)){

        }
    }

    private void handleLamportRequest(SocketChannel socketChannel, RequestHandler requestHandler) throws IOException {
        // 比较时钟大小，返回较大值
        int tmpTime = Integer.parseInt(requestHandler.getBody());
        tmpTime = Math.max(tmpTime,time.get());
        time.set(tmpTime);
        String response = HttpBuilder.buildResponse("200", "success", String.valueOf(time.get()));
        sendMessage(response, socketChannel);
        socketChannel.close();
    }

    public static void main(String[] args) throws IOException {
        // from args get aggregation server port ip
        // build socket connection(contain buffer-in and buffer-out)
        // with buffer-out: send http message(head, content:json[come from a file which need to be analysed])
        if (args.length == 4) {
            int port = Integer.parseInt(args[0]);
            String name = args[1];
            String aggrIp = args[2];
            int aggrPort = Integer.parseInt(args[3]);
            GETClient getClient = new GETClient(port, name, aggrIp, aggrPort);
            getClient.start();
        }
    }
}
