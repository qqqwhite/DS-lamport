import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Storage implements Serializable {

    private int latestTimestamp = 0;

    private final Map<String, Node> nodes = new HashMap<>();

    private final Map<String, Long> timeCounter = new HashMap<>();

    public void uploadWeatherInfo(String contentServerName, String weatherInfo, int timestamp){
        this.latestTimestamp = Math.max(latestTimestamp, timestamp);
        if (nodes.containsKey(contentServerName)){
            Node node = nodes.get(contentServerName);
            node.addInfo(weatherInfo, timestamp);
        }else {
            Node node = new Node(weatherInfo, timestamp);
            nodes.put(contentServerName, node);
        }
        timeCounter.put(contentServerName, System.currentTimeMillis());
    }

    public String readWeatherInfo(String contentServerName, int timestamp){
        if (!nodes.containsKey(contentServerName)){
            return null;
        }else {
            if (isTimeout(contentServerName)) {
                nodes.remove(contentServerName);
                timeCounter.remove(contentServerName);
                return null;
            } else {
                Node node = nodes.get(contentServerName);
                return node.getWeatherInfo(timestamp);
            }
        }
    }

    private boolean isTimeout(String contentServerName){
        long delta = System.currentTimeMillis() - timeCounter.get(contentServerName);
        return delta > 30 * 60 * 1000;
    }

    public boolean containsNode(String contentServerName){
        return nodes.containsKey(contentServerName);
    }

    public int getLatestTimestamp() {
        return latestTimestamp;
    }

    public void setLatestTimestamp(int latestTimestamp) {
        this.latestTimestamp = latestTimestamp;
    }
}

class Node implements Serializable {

    List<Info> weatherInfos = new ArrayList<>(); // 保障最新的放在最后面

    public Node(String weatherInfo, int timestamp) {
        Info info = new Info(timestamp, weatherInfo);
        weatherInfos.add(info);
    }

    public void addInfo(String weatherInfo, int timestamp) {
        Info info = new Info(timestamp, weatherInfo);
        weatherInfos.add(info);
    }

    public String getWeatherInfo(int timestamp) {
        for (int i = weatherInfos.size() - 1; i >= 0; i--) {
            Info info = weatherInfos.get(i);
            if (info.getTimestamp() <= timestamp) {
                return info.getWeatherInfo();
            }
        }
        return null;
    }

}

class Info implements Serializable {
    private String weatherInfo;

    private int timestamp;

    public Info(int timestamp, String weatherInfo) {
        this.timestamp = timestamp;
        this.weatherInfo = weatherInfo;
    }

    public String getWeatherInfo() {
        return weatherInfo;
    }

    public void setWeatherInfo(String weatherInfo) {
        this.weatherInfo = weatherInfo;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}