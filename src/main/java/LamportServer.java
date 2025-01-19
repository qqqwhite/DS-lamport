import com.google.gson.JsonElement;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class LamportServer {

    final Config config = new Config();

    final int port;

    final int aggrPort;

    final String aggrIp;

    final String name;

    final AtomicInteger time = new AtomicInteger(0); // CAS

    final Selector selector;

    final Map<String, String> ipPorts = new HashMap<>();

    public LamportServer(int port,String name, String aggrIp, int aggrPort) throws IOException {
        this.port = port;
        this.aggrPort = aggrPort;
        this.aggrIp = aggrIp;
        this.name = name;
        this.selector = Selector.open();
    }

    public LamportServer(int port,String name, String aggrIp, int aggrPort, boolean isTestingModel) throws IOException {
        this.port = port;
        this.aggrPort = aggrPort;
        this.aggrIp = aggrIp;
        this.name = name;
        this.selector = Selector.open();
        config.isTestingModel = isTestingModel;
    }
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
        getLamportTime();// 时钟+1
        if (config.isTestingModel) System.out.println(this.name+" operating at time: "+this.time.get());
    }

    protected void getLamportTime() throws IOException {
        // 向其他所有节点发送一个带本地时钟的请求消息
        // 接收response，其中包含其他节点的同步时钟，（本地+1，其他节点不+1）
        // 等待所有response返回后，确定当前时钟
        if (ipPorts.isEmpty()){
            time.set(time.get()+1);
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(ipPorts.size());
        for (Map.Entry<String, String> entry : ipPorts.entrySet()) {
            executorService.execute(() -> {
                try {
                    SocketChannel socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(true);
                    socketChannel.connect(new InetSocketAddress(Util.getIp(entry.getValue()), Util.getPort(entry.getValue())));
//                    socketChannel.register(this.selector, SelectionKey.OP_READ);
                    while(!socketChannel.finishConnect()){
                        Thread.yield();//fix
                    }
                    String request = HttpBuilder.builder()
                            .buildRequestLine(HttpBuilder.GET, "/lamport", HttpBuilder.version)
                            .buildRequestHeader(HttpBuilder.headerUserAgent, this.name)
                            .buildRequestHeader(HttpBuilder.headerContentType, HttpBuilder.headerTextType)
                            .buildRequestBody(String.valueOf(time.get()+1))
                            .builder();
                    sendMessage(request, socketChannel);
                    // ?
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int bytesRead = socketChannel.read(byteBuffer);
                    if (bytesRead > 0) {
                        byteBuffer.flip(); // 切换到读模式
                        byte[] byteArray = new byte[byteBuffer.remaining()];
                        byteBuffer.get(byteArray); // 从缓冲区获取数据
                        String response = new String(byteArray); // 转换为字符串
                        ResponseHandler responseHandler = new ResponseHandler(response);
                        int tmpTime = Integer.parseInt(responseHandler.getBody());
                        if (tmpTime>time.get()){
                            time.set(tmpTime);
                        }
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
        }catch (InterruptedException e){
            throw new RuntimeException("lamport error");
        }
    }

    protected void getAllAddr() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.connect(new InetSocketAddress(String.valueOf(this.aggrIp), this.aggrPort));
//        socketChannel.register(this.selector, SelectionKey.OP_READ);
        while(!socketChannel.finishConnect()){
            Thread.yield();//fix
        }
        Map<String, Object> map = new HashMap<>();
        map.put("type", this.getClass().getName());
        map.put("port", port);
        String request = HttpBuilder.builder()
                .buildRequestLine(HttpBuilder.PUT, "/connect", HttpBuilder.version)
                .buildRequestHeader(HttpBuilder.headerUserAgent, this.name)
                .buildRequestHeader(HttpBuilder.headerContentType, HttpBuilder.headerJsonType)
                .buildRequestBody(JsonHandler.transMap(map))
                .builder();
        sendMessage(request, socketChannel);
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        int bytesRead = socketChannel.read(byteBuffer);
        if (bytesRead > 0) {
            byteBuffer.flip();
            byte[] byteArray = new byte[byteBuffer.remaining()];
            byteBuffer.get(byteArray);
            String response = new String(byteArray);
            ResponseHandler responseHandler = new ResponseHandler(response);
            handleConnectResponse(socketChannel, responseHandler);
        }
    }

    protected void sendMessage(String message, SocketChannel socketChannel) throws IOException {
        if (socketChannel != null && socketChannel.isConnected()) {
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
            if (config.isTestingModel) System.out.println(this.name+" send: \n" + message);
        }
    }

    protected void server() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(this.port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        // waiting for lamport time info
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

    private void handleConnectResponse(SocketChannel socketChannel, HttpHandler httpHandler){
        ResponseHandler responseHandler = (ResponseHandler) httpHandler;
        int aggrServerTimestamp = Integer.parseInt(responseHandler.getMsg());
        time.set(aggrServerTimestamp);
        try {
            JsonObject jsonObject = JsonHandler.transStringToJson(responseHandler.getBody());
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (Util.getServerName(entry.getKey()).equals(this.name)) continue;
                ipPorts.put(entry.getKey(), entry.getValue().getAsString());
            }
            socketChannel.close();//TODO: 统一进行
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(Config.bufferSize);
        int bytesRead = socketChannel.read(byteBuffer);
        if (bytesRead == -1) {
            socketChannel.close();
            return;
        }
        byteBuffer.flip();
        String info = new String(byteBuffer.array());
        if (config.isTestingModel) {
            System.out.println(this.name + " received: \n" + info);
        }
        handleRequest(socketChannel, info);
    }

    public String handleResponse(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Config.bufferSize);
        int bytesRead = socketChannel.read(byteBuffer);
        if (bytesRead == -1) {
            socketChannel.close();
            return null;
        }
        byteBuffer.flip();
        return new String(byteBuffer.array());
    }

    /**
     * 对request进行自定义响应
     * @param socketChannel
     * @param request
     * @throws IOException
     */
    protected abstract void handleRequest(SocketChannel socketChannel, String request) throws IOException;
}
