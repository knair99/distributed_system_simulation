package networking.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class StatusRequestHandler {
    public static void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        String responseMessage = "Replica is alive\n";
        ResponseHandler.sendResponse(responseMessage.getBytes(), exchange);
    }
}
