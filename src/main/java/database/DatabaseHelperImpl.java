package database;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelperImpl implements DatabaseHelper {

    public static MongoDatabase connectToMongoDB(String url, String dbName) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
        return mongoClient.getDatabase(dbName);
    }

    public static void generateBulkData(int numberOfWrites, String offset) {
        String collectionName = DatabaseInstance.getInstance().getCollectionName();
        MongoCollection<Document> collection = DatabaseInstance.getInstance().getDatabase().getCollection(collectionName);

        List<Document> documents = new ArrayList<>();
        for (int dataValue = 0; dataValue < numberOfWrites; dataValue++) {
            Document document = new Document();
            document.append("name", offset + "_" + dataValue);
            documents.add(document);
        }
        collection.insertMany(documents);
        System.out.println("Bulk inserted: " + numberOfWrites + " documents in collection: " + "collectionName");
    }

    public static void writeDataToDatabase(String dataString) {
        String collectionName = DatabaseInstance.getInstance().getCollectionName();
        MongoCollection<Document> collection = DatabaseInstance.getInstance().getDatabase().getCollection(collectionName);
        Document document = new Document();
        document.append("name", collectionName + "_data_" + dataString);
        collection.insertOne(document);
        System.out.println("Wrote record: " + dataString + " to collection: " + collectionName);
    }

    public static String readDataFromDatabase(String dataString) {
        String collectionName = DatabaseInstance.getInstance().getCollectionName();
        MongoCollection<Document> collection = DatabaseInstance.getInstance().getDatabase().getCollection(collectionName);
        Document doc = collection.find().sort(new Document("_id", -1)).first();
        System.out.println("Wrote record: " + dataString + " to collection: " + collectionName);
        return doc.toString();
    }
}
