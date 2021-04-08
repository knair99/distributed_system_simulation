package cluster.management;

import org.apache.zookeeper.KeeperException;

public interface ReplicaRegistrationHelper {

    void unRegisterLeaderFromCluster() throws KeeperException, InterruptedException;

    void registerFollowerToCluster();
}
