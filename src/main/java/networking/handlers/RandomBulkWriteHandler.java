package networking.handlers;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import networking.WebServer;
import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RandomBulkWriteHandler {

    public static final int NUMBER_OF_WRITES = 10;
    private static final String MONGO_DB_URL = "mongodb://127.0.0.1:29017";
    private static final String DB_NAME = "testdb";
    private static final String COLLECTION_NAME = "data";
    private static final Random random = new Random();

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

        boolean isDebugMode = headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true");

        long startTime = System.nanoTime();

        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        byte[] responseBytes = writeBulkRandomDataToDatabase(requestBytes);
        long finishTime = System.nanoTime();

        if (isDebugMode) {
            String debugMessage = String.format("Operation took %d ns", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        ResponseHandler.sendResponse(responseBytes, exchange);
    }

    private static byte[] writeBulkRandomDataToDatabase(byte[] requestBytes) {
        MongoDatabase mongoDatabase = connectToMongoDB(MONGO_DB_URL, DB_NAME);
        int offset = Integer.parseInt(new String(requestBytes));
        generateBulkData(NUMBER_OF_WRITES, mongoDatabase, COLLECTION_NAME, offset);
        return String.format("Generated bulk data!").getBytes();
    }

    private static MongoDatabase connectToMongoDB(String url, String dbName) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
        return mongoClient.getDatabase(dbName);
    }

    private static void generateBulkData(int numberOfWrites, MongoDatabase database, String collectionName, int offset) {
        MongoCollection<Document> collection = database.getCollection(collectionName);

        List<Document> documents = new ArrayList<>();
        for (int dataValue = 0; dataValue < numberOfWrites; dataValue++) {
            Document document = new Document();
            document.append("name", dataValue + offset);
            documents.add(document);
        }
        collection.insertMany(documents);
    }
}
