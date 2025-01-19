import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AggregationServer {

    final Config config = new Config();

    final int port;

    Storage storage2;
    // TODO:1 timestamp

    final Map<String, Object> ipPortMap = new HashMap<>();

    private AtomicBoolean uploadLock = new AtomicBoolean(false); // false unlock, true lock

    private Selector selector;

    public AggregationServer(int port) throws IOException {
        this.port = port;
        recoverData();
    }

    public AggregationServer(int port, boolean isTestingModel) throws IOException {
        this.port = port;
        config.isTestingModel = isTestingModel;
        recoverData();
    }

    public void start() throws IOException {
        selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(this.port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Listening on port " + port);
        // finish the initialization
        while (true) {
            selector.select();
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();
                if (key.isAcceptable()) {
                    // build a new connection
                    ServerSocketChannel socketChannel = (ServerSocketChannel) key.channel();
                    SocketChannel channel = socketChannel.accept();
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    // deal with info
                    handleRead(key);
                }
            }
        }
    }

    private void persistenceData(){
        Util.serializeObject(this.storage2, Config.storageFilename);
    }
    private void recoverData(){
        Object object = Util.deserializeObject(Config.storageFilename);
        if (object instanceof Storage){
            this.storage2 = (Storage) object;
        }else {
            this.storage2 = new Storage();
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(Config.bufferSize);
        int bytesRead = channel.read(byteBuffer);
        if (bytesRead == -1) {
            channel.close();
            return;
        }
        byteBuffer.flip();
        String request = new String(byteBuffer.array());
        if (config.isTestingModel) {
            System.out.println("AggregationServer received request: \n" + request);
        }
        RequestHandler requestHandler = new RequestHandler(request);
        if (requestHandler.getRequestType().equals(HttpBuilder.GET)) {
            handleGETRequest(requestHandler, channel);
        }else if (requestHandler.getRequestType().equals(HttpBuilder.PUT)) {
            if (requestHandler.getUrl().equals("/connect")){
                handlerConnectRequest(requestHandler, channel);
                System.out.println("Build connect with "+requestHandler.getHeaderInfo(HttpBuilder.headerUserAgent));
            }else if (requestHandler.getUrl().equals("/weather.json")){
                handleWeatherPutRequest(requestHandler, channel);
                System.out.println("Receive weather info from "+requestHandler.getHeaderInfo(HttpBuilder.headerUserAgent));
            }
        }else {
            handleUnexpectedRequest(requestHandler, channel);
        }
    }

    private void handleUnexpectedRequest(RequestHandler requestHandler, SocketChannel channel) throws IOException {
        String response = HttpBuilder.buildResponse("400", "ERROR");
        channel.write(ByteBuffer.wrap(response.getBytes()));
        channel.close();
    }

    private void handleGETRequest(RequestHandler requestHandler, SocketChannel channel) throws IOException {
        String url = requestHandler.getUrl();
        if (url.startsWith("/weather")){
            String contentServerName = url.substring("/weather/".length());
            handleWeatherGETRequest(contentServerName, requestHandler, channel);
            System.out.println("Trans weather info to "+requestHandler.getHeaderInfo(HttpBuilder.headerUserAgent));
        }else if (url.startsWith("/uploadLock")){
            handleLockRequest(requestHandler, channel);
        }else if (url.startsWith("/uploadUnlock")){
            handleUnlockRequest(requestHandler, channel);
        }
    }

    private void handleUnlockRequest(RequestHandler requestHandler, SocketChannel channel) throws IOException {
        if (uploadLock.get()) {
            uploadLock.set(false);
            String response = HttpBuilder.buildResponse("200", "UNLOCKED");
            channel.write(ByteBuffer.wrap(response.getBytes()));
            channel.close();
        }else {
            String response = HttpBuilder.buildResponse("201", "IS UNLOCKED");
            channel.write(ByteBuffer.wrap(response.getBytes()));
            channel.close();
        }
    }

    private void handleLockRequest(RequestHandler requestHandler, SocketChannel channel) throws IOException {
        synchronized (uploadLock) {
            if (!uploadLock.get()) {
                uploadLock.set(true);
                String response = HttpBuilder.buildResponse("200", "LOCKED");
                channel.write(ByteBuffer.wrap(response.getBytes()));
                channel.close();
            }else {
                String response = HttpBuilder.buildResponse("205", "ERROR");
                channel.write(ByteBuffer.wrap(response.getBytes()));
            }
        }
    }

    private void handleWeatherGETRequest(String contentServerName, RequestHandler requestHandler, SocketChannel channel) throws IOException {
        int timestamp = Integer.parseInt(requestHandler.getParam(HttpBuilder.paramTimestamp));
        if (storage2.containsNode(contentServerName)){
            String response = HttpBuilder.buildResponse("200", "OK", storage2.readWeatherInfo(contentServerName, timestamp));
            channel.write(ByteBuffer.wrap(response.getBytes()));
            channel.close();
        }else {
            String response = HttpBuilder.buildResponse("204", "NO-CONTENT");
            channel.write(ByteBuffer.wrap(response.getBytes()));
            channel.close();
        }
    }

    private void handleWeatherPutRequest(RequestHandler requestHandler, SocketChannel channel) throws IOException {
        String info = requestHandler.getBody();
        if (JsonHandler.checkJson(info)){
            String contentServerName = requestHandler.getHeaderInfo(HttpBuilder.headerUserAgent);
            int timestamp = Integer.parseInt(requestHandler.getParam(HttpBuilder.paramTimestamp));
            String response;
            if (!storage2.containsNode(contentServerName)){
                response = HttpBuilder.buildResponse("201", "HTTP-CREATED");
            }else {
                response = HttpBuilder.buildResponse("200", "OK");
            }
            storage2.uploadWeatherInfo(contentServerName, info, timestamp);
            channel.write(ByteBuffer.wrap(response.getBytes()));
            channel.close();
            persistenceData();
        }else {
            String response = HttpBuilder.buildResponse("500", "Internal Server Error");
            channel.write(ByteBuffer.wrap(response.getBytes()));
            channel.close();
        }

    }

    private void handlerConnectRequest(RequestHandler requestHandler, SocketChannel channel) throws IOException {
        String info = requestHandler.getBody();
        String clientName = requestHandler.getHeaderInfo(HttpBuilder.headerUserAgent);
        JsonObject jsonObject = JsonHandler.transStringToJson(info);
        String clientIP = channel.getRemoteAddress().toString().split(":")[0];
        clientIP = clientIP.replace("/", "");
        String type = jsonObject.get("type").toString().replace("\"","");
        String clientPort = jsonObject.get("port").toString();
        if (config.isTestingModel) System.out.println("AggregationServer build connected with: " + clientIP + ":" + clientPort);
        synchronized (ipPortMap) {
            String response = HttpBuilder.buildResponse("200", String.valueOf(storage2.getLatestTimestamp()), JsonHandler.transMap(ipPortMap));
            if (config.isTestingModel) System.out.println("AggregationServer send response: \n" + response);
            channel.write(ByteBuffer.wrap(response.getBytes()));
            channel.close();
            ipPortMap.put(type+":"+clientName, clientIP + ":" + clientPort);
        }
    }


    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            int port = Integer.parseInt(args[0]);
            AggregationServer aggregationServer = new AggregationServer(port);
            aggregationServer.start();
        }
    }


}
