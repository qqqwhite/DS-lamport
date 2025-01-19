import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ServerTest {

    @Test
    public void test_multiUpload() throws InterruptedException {
        final AggregationServer[] aggregationServer = new AggregationServer[1];
        final ContentServer[] contentServer = new ContentServer[2];
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread() {
            public void run() {
                try {
                    aggregationServer[0] = new AggregationServer(Config.aggrPort+4, true);
                    latch.countDown();
                    aggregationServer[0].start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
        latch.await();
        for (int i = 0; i < 2; i++) {
            int finalI = i;
            new Thread() {
                public void run() {
                    try {
                        contentServer[0] = new ContentServer(8050+ finalI, "c"+finalI, String.valueOf(finalI), Config.aggrIP, Config.aggrPort + 4);
                        contentServer[0].start();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
        Thread.sleep(1000);
        assertEquals(2, aggregationServer[0].ipPortMap.size());
    }

    @Test
    public void test_uploadAndGet() throws InterruptedException, IOException {
        final AggregationServer[] aggregationServer = new AggregationServer[1];
        final ContentServer[] contentServer = new ContentServer[1];
        final GETClient[] getClients = new GETClient[1];
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread() {
            public void run() {
                try {
                    aggregationServer[0] = new AggregationServer(Config.aggrPort+3, true);
                    latch.countDown();
                    aggregationServer[0].start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
        latch.await();
        new Thread() {
            public void run() {
                try {
                    contentServer[0] = new ContentServer(8040, "c1", "0", Config.aggrIP, Config.aggrPort+3);
                    contentServer[0].start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
        Thread.sleep(1000);
        new Thread() {
            public void run() {
                try {
                    getClients[0] = new GETClient(8050, "g1", Config.aggrIP, Config.aggrPort+3);
                    getClients[0].start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
        Thread.sleep(1000);
        assertEquals(contentServer[0].weatherInfo, getClients[0].getWeatherInfo("c1"));
    }

    @Test
    public void test_uploadWeatherInfo() throws InterruptedException {
        final AggregationServer[] aggregationServer = new AggregationServer[1];
        final ContentServer[] contentServer = new ContentServer[1];
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread() {
            public void run() {
                try {
                    aggregationServer[0] = new AggregationServer(Config.aggrPort, true);
                    latch.countDown();
                    aggregationServer[0].start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
        latch.await();
        new Thread() {
            public void run() {
                try {
                    contentServer[0] = new ContentServer(8030, "c1", "0", Config.aggrIP, Config.aggrPort);
                    contentServer[0].start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
        Thread.sleep(1000);
        assertEquals(1, aggregationServer[0].ipPortMap.size());
        assertTrue(aggregationServer[0].storage2.containsNode("c1"));
    }

    @Test
    public void test_aggregationServerDataRecover() throws InterruptedException {
        File file = new File(Config.storageFilename);
        boolean isExists = file.exists();
        final AggregationServer[] aggregationServer = new AggregationServer[1];
        final CountDownLatch latch = new CountDownLatch(1);

        new Thread() {
            public void run() {
                try {
                    aggregationServer[0] = new AggregationServer(Config.aggrPort+1, true);
                    latch.countDown();
                    aggregationServer[0].start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
        latch.await();
        if (isExists) {
            Object object = Util.deserializeObject(Config.storageFilename);
            Storage storage = (Storage) object;
            assertEquals(storage.getLatestTimestamp(), aggregationServer[0].storage2.getLatestTimestamp());
        } else {
            assertEquals(0, aggregationServer[0].storage2.getLatestTimestamp());
        }
    }

    @Test
    public void test_multithreading() throws InterruptedException {
        int contentCount = 2;
        int getCount = 3;
        int port = 8081;
        final AggregationServer[] aggregationServer = new AggregationServer[1];
        List<LamportServer> lamportServers = new ArrayList<>();
        new Thread(){
            public void run(){
                try {
                    aggregationServer[0] = new AggregationServer(Config.aggrPort+2, true);
                    aggregationServer[0].start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
        Thread.sleep(1000);
        for (int i = 0; i < contentCount; i++) {
            int finalI = i;
            new Thread() {
                public void run() {
                    try {
                        ContentServer contentServer = new ContentServer(8010 + finalI, "ContentServer" + finalI, String.valueOf(finalI), Config.aggrIP, Config.aggrPort+2, true);
                        contentServer.start();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
        Thread.sleep(1000);
        for (int i = 0; i < getCount; i++) {
            int finalI = i;
            new Thread(){
                public void run(){
                    try {
                        GETClient getClient = new GETClient(8020+ finalI, "GETClient"+ finalI, Config.aggrIP, Config.aggrPort+2, true);
                        getClient.start();
                        lamportServers.add(getClient);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
        Thread.sleep(1000);
    }

}
