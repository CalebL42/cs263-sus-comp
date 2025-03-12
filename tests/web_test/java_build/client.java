import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// Client Implementation
class client {
    private static final AtomicInteger totalRequests = new AtomicInteger(0);
    private static final AtomicInteger successfulRequests = new AtomicInteger(0);
    private static final AtomicInteger failedRequests = new AtomicInteger(0);
    private static final AtomicLong totalResponseTime = new AtomicLong(0);

    public static void main(String[] args) throws InterruptedException {
        String host = "http://192.168.0.54:8080";
        List<String> paths = Arrays.asList("/index.html", "/images/amogus.png", "/images/pumpkin.jpg", "/js/script.js", "/css/style.css");
        int numClients = 50;
        int requestsPerClient = 100;
        int delayMs = 500;
        
        ExecutorService executor = Executors.newFixedThreadPool(numClients);
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numClients; i++) {
            executor.execute(() -> runClient(host, paths, requestsPerClient, delayMs));
        }
        
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(100);
        }
        
        long totalDuration = System.currentTimeMillis() - startTime;
        System.out.println("\n----- Simulation Results -----");
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Successful requests: " + successfulRequests);
        System.out.println("Failed requests: " + failedRequests);
        if (totalRequests.get() > 0) {
            double avgResponseTime = (double) totalResponseTime.get() / totalRequests.get();
            System.out.println("Average response time: " + avgResponseTime + " ms");
            double requestsPerSecond = (double) totalRequests.get() * 1000 / totalDuration;
            System.out.println("Throughput: " + requestsPerSecond + " requests/second");
        }
        System.out.println("Total simulation time: " + totalDuration + " ms");
    }

    private static void runClient(String host, List<String> paths, int numRequests, int delayMs) {
        for (int i = 0; i < numRequests; i++) {
            String url = host + paths.get(i % paths.size());
            long start = System.currentTimeMillis();
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                long duration = System.currentTimeMillis() - start;
                totalRequests.incrementAndGet();
                totalResponseTime.addAndGet(duration);
                
                if (responseCode == 200) {
                    successfulRequests.incrementAndGet();
                    System.out.println("Request to " + url + " succeeded in " + duration + "ms");
                } else {
                    failedRequests.incrementAndGet();
                    System.out.println("Request to " + url + " failed with status " + responseCode);
                }
                conn.disconnect();
            } catch (IOException e) {
                failedRequests.incrementAndGet();
                System.out.println("Request to " + url + " failed with error: " + e.getMessage());
            }
            
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}