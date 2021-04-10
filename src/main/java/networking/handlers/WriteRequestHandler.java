package networking.handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import networking.SyncCoordinator;
import networking.WebServer;

import java.io.IOException;
import java.util.Arrays;

public class WriteRequestHandler {


    public static void handleWriteRequest(WebServer webServer, HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }

        Headers headers = exchange.getRequestHeaders();
        if (headers.containsKey("X-Test") && headers.get("X-Test").get(0).equalsIgnoreCase("true")) {
            String dummyResponse = "Test successful\n";
            ResponseHandler.sendResponse(dummyResponse.getBytes(), exchange);
            return;
        }

        boolean isDebugMode = false;
        if (headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
            isDebugMode = true;
        }

        long startTime = System.nanoTime();

        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        byte[] responseBytes = saveData(requestBytes);
        long finishTime = System.nanoTime();

        if (isDebugMode) {
            String debugMessage = String.format("Operation took %d ns", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        // Depending on replication strategy, do sync alerting vs async alerting here
        syncAlertFollowers();

        ResponseHandler.sendResponse(responseBytes, exchange);
    }

    private static void syncAlertFollowers() {
        System.out.println("Alerting followers synchronously: ");
        SyncCoordinator syncCoordinator = new SyncCoordinator();
        syncCoordinator.alertAllFollowersSync();
    }

    private static void asyncAlertFollowers() {
        SyncCoordinator syncCoordinator = new SyncCoordinator();
        syncCoordinator.alertAllFollowersSync();
    }

    // TODO: Persist into MongoDB, Identify self
    static byte[] saveData(byte[] requestBytes) {
        String bodyString = new String(requestBytes);

        return String.format("Data is saved").getBytes();
    }
}
