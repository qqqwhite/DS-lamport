import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class ContentServer extends LamportServer{

    String weatherInfo;

    final String jsonFilePath;

    public ContentServer(int port, String name, String jsonFilePath, String aggrIp, int aggrPort) throws IOException {
        super(port, name, aggrIp, aggrPort);
        this.jsonFilePath = jsonFilePath;
    }

    public ContentServer(int port, String name, String jsonFilePath, String aggrIp, int aggrPort, boolean isTestingModel) throws IOException {
        super(port, name, aggrIp, aggrPort, isTestingModel);
        this.jsonFilePath = jsonFilePath;
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
            weatherInfo = JsonHandler.loadFile(jsonFilePath);
            if (weatherInfo.isEmpty()) {
                System.err.println("read no weather info!");
                System.exit(1);
            }
            uploadLock();
            getLamportTime();
            uploadWeatherInfo();
            uploadUnlock();
        }else {
            while (true){
                weatherInfo = JsonHandler.loadFile(jsonFilePath);
                if (weatherInfo.isEmpty()) {
                    System.err.println("read no weather info!");
                    System.exit(1);
                }
                uploadLock();
                getLamportTime();
                uploadWeatherInfo();
                uploadUnlock();
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void uploadUnlock() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.connect(new InetSocketAddress(String.valueOf(this.aggrIp), this.aggrPort));
        String request = HttpBuilder.builder()
                .buildRequestLine(HttpBuilder.GET, "/uploadUnlock", HttpBuilder.version)
                .buildRequestHeader(HttpBuilder.headerUserAgent, this.name)
                .buildRequestHeader(HttpBuilder.headerContentType, HttpBuilder.headerJsonType)
                .builder();
        while (true) {
            socketChannel.write(ByteBuffer.wrap(request.getBytes()));
            String response = handleResponse(socketChannel);
            ResponseHandler responseHandler = new ResponseHandler(response);
            if (responseHandler.getCode().equals("200") || responseHandler.getCode().equals("201")) break;
        }
        if (config.isTestingModel) System.out.println(name+" release upload lock");
    }

    private void uploadLock() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.connect(new InetSocketAddress(String.valueOf(this.aggrIp), this.aggrPort));
        String request = HttpBuilder.builder()
                .buildRequestLine(HttpBuilder.GET, "/uploadLock", HttpBuilder.version)
                .buildRequestHeader(HttpBuilder.headerUserAgent, this.name)
                .buildRequestHeader(HttpBuilder.headerContentType, HttpBuilder.headerJsonType)
                .builder();
        int waitingBreak = 200;
        while (true){
            socketChannel.write(ByteBuffer.wrap(request.getBytes()));
            String response = handleResponse(socketChannel);
            ResponseHandler responseHandler = new ResponseHandler(response);
            if (responseHandler.getCode().equals("200")) break;
            try {
                Thread.sleep(waitingBreak);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            waitingBreak += (int) (Math.random() * waitingBreak);
        }
        if (config.isTestingModel) System.out.println(name+" get upload lock");
    }

    private void uploadWeatherInfo() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.connect(new InetSocketAddress(String.valueOf(this.aggrIp), this.aggrPort));
        while (!socketChannel.finishConnect()) {
            Thread.yield();
        }
//        String request = HttpBuilder.buildPUT("/weather.json", info, this.name);
        Map<String, String> params = new HashMap<>();
        params.put(HttpBuilder.paramTimestamp, String.valueOf(time));
        String request = HttpBuilder.builder()
                .buildRequestLine(HttpBuilder.PUT, HttpBuilder.buildUrl("/weather.json", params), HttpBuilder.version)
                .buildRequestHeader(HttpBuilder.headerUserAgent, this.name)
                .buildRequestHeader(HttpBuilder.headerContentType, HttpBuilder.headerJsonType)
                .buildRequestBody(weatherInfo)
                .builder();
        if (config.isTestingModel) System.out.println("ContentServer request: \n" + request);
        socketChannel.write(ByteBuffer.wrap(request.getBytes()));
        String response = handleResponse(socketChannel);
        ResponseHandler responseHandler = new ResponseHandler(response);
        StringBuilder stringBuilder = new StringBuilder()
                .append(this.name)
                .append(" upload weather info with response:\n")
                .append("Code: ").append(responseHandler.getCode()).append("\n")
                .append("Message: ").append(responseHandler.getMsg()).append("\n");
        System.out.println(stringBuilder);
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

    public static void main(String[] args) throws IOException {
        if (args.length == 5) {
            int port = Integer.parseInt(args[0]);
            String name = args[1];
            String jsonFilePath = args[2];
            String aggrIp = args[3];
            int aggrPort = Integer.parseInt(args[4]);
            ContentServer contentServer = new ContentServer(port, name, jsonFilePath, aggrIp, aggrPort);
            contentServer.start();
        }
    }
}
