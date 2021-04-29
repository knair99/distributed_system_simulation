package networking.handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import config.Configuration;
import networking.WebServer;
import networking.database.DatabaseHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SyncRequestHandler {

    public static void handleSyncRequest(WebServer webServer, HttpExchange exchange) throws IOException {
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

        long startTime = System.nanoTime();
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        byte[] responseBytes = syncData(requestBytes);
        long finishTime = System.nanoTime();

        boolean isDebugMode = headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true");
        if (isDebugMode) {
            String debugMessage = String.format("Operation took %d ns", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        ResponseHandler.sendResponse(responseBytes, exchange);
    }

    static byte[] syncData(byte[] requestBytes) {
        String bodyString = new String(requestBytes);

        System.out.println("Syncing from leader finished");
        DatabaseHelper.writeDataToDatabase(bodyString);
        System.out.println("Replication complete");

        return String.format("Ack from follower: " + Configuration.getPort()).getBytes(StandardCharsets.UTF_8);
    }
}
