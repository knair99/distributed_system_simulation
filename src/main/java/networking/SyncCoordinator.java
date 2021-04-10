package networking;

import cluster.management.DataCenterRegistry;

import java.util.List;

public class SyncCoordinator {

    public void alertAllFollowersSync() {

        WebClient webClient = new WebClient();
        DataCenterRegistry dataCenterRegistry = DataCenterRegistry.getInstance();
        try {
            List<String> allFollowerAddresses = dataCenterRegistry.getAllRegisteredReplicaAddresses();
            for(String followerAddress : allFollowerAddresses){
                System.out.println("\t Sync alert follower: " +  followerAddress + "/sync");
                webClient.sendMessage(followerAddress + "/sync", "hello".getBytes());
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
