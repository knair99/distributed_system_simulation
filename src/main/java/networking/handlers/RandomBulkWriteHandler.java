package networking.handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import networking.WebServer;
import networking.database.DatabaseHelper;

import java.io.IOException;
import java.util.Arrays;

public class RandomBulkWriteHandler {

    public static final int NUMBER_OF_WRITES = 10000;

    public static void handleRandomBulkWrite(WebServer webServer, HttpExchange exchange) throws IOException {

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
        byte[] responseBytes = writeBulkRandomDataToDatabase(requestBytes);
        long finishTime = System.nanoTime();

        boolean isDebugMode = headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true");
        if (isDebugMode) {
            String debugMessage = String.format("Operation took %d ns", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        ResponseHandler.sendResponse(responseBytes, exchange);
    }

    private static byte[] writeBulkRandomDataToDatabase(byte[] requestBytes) {
        String offset = new String(requestBytes);
        DatabaseHelper.generateBulkData(NUMBER_OF_WRITES, offset);
        return String.format("Generated bulk data!").getBytes();
    }

}
