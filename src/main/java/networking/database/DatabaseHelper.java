package networking.database;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    public static MongoDatabase connectToMongoDB(String url, String dbName) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
        return mongoClient.getDatabase(dbName);
    }

    public static void generateBulkData(int numberOfWrites, MongoDatabase database, String collectionName,
                                        String offset) {
        MongoCollection<Document> collection = database.getCollection(collectionName);

        List<Document> documents = new ArrayList<>();
        for (int dataValue = 0; dataValue < numberOfWrites; dataValue++) {
            Document document = new Document();
            document.append("name", offset + "_" + dataValue);
            documents.add(document);
        }
        collection.insertMany(documents);
        System.out.println("Bulk inserted: " + numberOfWrites + " documents in DB:" + database + ", " +
                ", collection:" + collectionName);
    }

    public static void writeRecordToDatabase(MongoDatabase database, String collectionName,
                                             String dataString) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Document document = new Document();
        document.append("name", collectionName + "_data_" + dataString);
        collection.insertOne(document);
        System.out.println("Wrote record: " + dataString + " to collection: " + collectionName);
    }
}
