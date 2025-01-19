import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;

public class Util {
    public static String getIp(String ipPort){
        return ipPort.split(":")[0];
    }
    public static int getPort(String ipPort){
        return Integer.parseInt(ipPort.split(":")[1]);
    }
    public static String getClassName(String str) {
        return str.split(":")[0];
    }
    public static String getServerName(String str) {
        return str.split(":")[1];
    }

    public static void serializeObject(Object object, String filename){
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(object);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object deserializeObject(String filename){
        Object object = null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            object = in.readObject();
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            return null;
        }
        return object;
    }
}
