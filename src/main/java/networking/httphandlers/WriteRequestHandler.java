package networking.httphandlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import config.Configuration;
import networking.SyncCoordinator;
import networking.WebServer;
import database.DatabaseHelperImpl;

import java.io.IOException;
import java.net.UnknownHostException;
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

        boolean isDebugMode = headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true");

        long startTime = System.nanoTime();

        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        byte[] responseBytes = saveData(requestBytes);
        long finishTime = System.nanoTime();

        if (isDebugMode) {
            String debugMessage = String.format("Operation took %d ns", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        // Depending on replication strategy, do sync alerting vs async alerting here
        if (Configuration.getInstance().getReplicationStrategy() == "sync") {
            syncAlertFollowers();
        } else {
            asyncAlertFollowers(requestBytes);
        }

        ResponseHandler.sendResponse(responseBytes, exchange);
    }

    private static void syncAlertFollowers() {
        System.out.println("Alerting followers synchronously: ");
        SyncCoordinator syncCoordinator = new SyncCoordinator();
        syncCoordinator.alertAllFollowersSync();
    }

    private static void asyncAlertFollowers(byte[] requestBytes) {
        System.out.println("Alerting followers asynchronously: ");
        SyncCoordinator syncCoordinator = new SyncCoordinator();
        syncCoordinator.alertAllFollowersAsync(requestBytes);
    }

    // TODO: Persist into MongoDB, Identify self
    static byte[] saveData(byte[] requestBytes) throws UnknownHostException {
        String bodyString = new String(requestBytes);
        DatabaseHelperImpl.writeDataToDatabase(bodyString);
        return String.format("Data is saved").getBytes();
    }
}
