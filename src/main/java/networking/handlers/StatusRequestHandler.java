package networking.handlers;

import com.sun.net.httpserver.HttpExchange;
import config.Configuration;

import java.io.IOException;

public class StatusRequestHandler {
    public static void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        String responseMessage = Configuration.isIsLeader() ? "Leader" : "Follower";
        ResponseHandler.sendResponse(responseMessage.getBytes(), exchange);
    }
}
