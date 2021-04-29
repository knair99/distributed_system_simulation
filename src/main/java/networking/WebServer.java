package networking;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import networking.httphandlers.RandomBulkWriteHandler;
import networking.httphandlers.ReadRequestHandler;
import networking.httphandlers.StatusRequestHandler;
import networking.httphandlers.SyncRequestHandler;
import networking.httphandlers.WriteRequestHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebServer {
    private static final String WRITE_ENDPOINT = "/write";
    private static final String READ_ENDPOINT = "/read";
    private static final String STATUS_ENDPOINT = "/status";
    private static final String SYNC_ENDPOINT = "/sync";
    private static final String BULK_RANDOM_WRITE = "/bulkrandomwrite";

    private final int port;
    private HttpServer server;

    public WebServer(int port) {
        this.port = port;
    }

    public void startServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext writeContext = server.createContext(WRITE_ENDPOINT);
        HttpContext readContext = server.createContext(READ_ENDPOINT);
        HttpContext syncContext = server.createContext(SYNC_ENDPOINT);
        HttpContext bulkRandomWriteContext = server.createContext(BULK_RANDOM_WRITE);


        statusContext.setHandler(StatusRequestHandler::handleStatusCheckRequest);
        writeContext.setHandler(exchange -> WriteRequestHandler.handleWriteRequest(WebServer.this, exchange));
        readContext.setHandler(exchange -> ReadRequestHandler.handleReadRequest(WebServer.this, exchange));
        syncContext.setHandler(exchange -> SyncRequestHandler.handleSyncRequest(WebServer.this, exchange));
        bulkRandomWriteContext.setHandler(exchange -> RandomBulkWriteHandler.handleRandomBulkWrite(WebServer.this, exchange));

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }


}
