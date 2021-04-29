package networking.database;

import com.mongodb.client.MongoDatabase;
import config.Configuration;

public class DatabaseInstance {

    private static final String MONGO_DB_URL = "mongodb://127.0.0.1:29017"; // IP Address of the "mongos" router
    private static final String DB_NAME = "testdb";
    private static final String COLLECTION_NAME_PREFIX = "data_";
    public static DatabaseInstance instance;
    private final String collectionName = COLLECTION_NAME_PREFIX + Configuration.getPort();
    private final MongoDatabase database;

    private DatabaseInstance() {
        database = DatabaseHelper.connectToMongoDB(MONGO_DB_URL, DB_NAME);
    }

    public static DatabaseInstance getInstance() {
        if (instance == null) {
            instance = new DatabaseInstance();
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public String getCollectionName() {
        return collectionName;
    }

}
