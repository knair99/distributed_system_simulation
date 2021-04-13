package networking;

import cluster.management.DataCenterRegistry;
import org.apache.kafka.clients.producer.Producer;
import queue.AsyncReplicationEventProducer;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class SyncCoordinator {

    public void alertAllFollowersSync() {

        WebClient webClient = new WebClient();
        DataCenterRegistry dataCenterRegistry = DataCenterRegistry.getInstance();
        try {
            List<String> allFollowerAddresses = dataCenterRegistry.getAllRegisteredReplicaAddresses();
            for (String followerAddress : allFollowerAddresses) {
                System.out.println("\t Sync alert follower: " + followerAddress + "/sync");
                webClient.sendMessage(followerAddress + "/sync", "hello".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void alertAllFollowersAsync() {

        AsyncReplicationEventProducer asyncReplicationEventProducer = new AsyncReplicationEventProducer();
        Producer<Long, String> kafkaProducer =
                asyncReplicationEventProducer.createKafkaProducer(AsyncReplicationEventProducer.BOOTSTRAP_SERVERS);
        try {
            asyncReplicationEventProducer.publishMessages(1, kafkaProducer);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            kafkaProducer.flush();
            kafkaProducer.close();
        }
    }
}
