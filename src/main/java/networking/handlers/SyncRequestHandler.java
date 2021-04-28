package networking.handlers;

import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import config.Configuration;
import networking.WebServer;
import networking.database.DatabaseHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SyncRequestHandler {

    private static final String MONGO_DB_URL = "mongodb://127.0.0.1:29017"; // IP Address of the "mongos" router
    private static final String DB_NAME = "testdb";
    private static final String COLLECTION_NAME_PREFIX = "data_";

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

    // TODO: Persist into MongoDB, Identify self
    static byte[] syncData(byte[] requestBytes) {
        String bodyString = new String(requestBytes);
        String hostName = "localhost_";

        System.out.println("Syncing from leader finished");
        MongoDatabase database = DatabaseHelper.connectToMongoDB(MONGO_DB_URL, DB_NAME);
        String collectionName = COLLECTION_NAME_PREFIX + hostName + Configuration.getPort();
        DatabaseHelper.writeRecordToDatabase(database, collectionName,
                bodyString);
        System.out.println("Replication complete");

        return String.format("Ack from follower: " + hostName).getBytes(StandardCharsets.UTF_8);
    }
}
