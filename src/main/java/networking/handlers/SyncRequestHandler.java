package networking.handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import networking.WebServer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

        boolean isDebugMode = false;
        if (headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
            isDebugMode = true;
        }

        long startTime = System.nanoTime();

        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        byte[] responseBytes = syncData(requestBytes);
        long finishTime = System.nanoTime();

        if (isDebugMode) {
            String debugMessage = String.format("Operation took %d ns", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        ResponseHandler.sendResponse(responseBytes, exchange);
    }

    // TODO: Persist into MongoDB, Identify self
    static byte[] syncData(byte[] requestBytes) {
        String bodyString = new String(requestBytes);
        String currentAddress = "unidentified";

        System.out.println("Syncing from leader finished");
        try {
            currentAddress = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return String.format("Ack from follower: " + currentAddress).getBytes(StandardCharsets.UTF_8);
    }
}
