package cluster.management;

import org.apache.zookeeper.KeeperException;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ReplicaRegistrationHelperImpl implements ReplicaRegistrationHelper {
    private final DataCenterRegistry dataCenterRegistry;
    private final int port;

    public ReplicaRegistrationHelperImpl(DataCenterRegistry dataCenterRegistry, int port) {
        this.dataCenterRegistry = dataCenterRegistry;
        this.port = port;
    }

    //If node is a leader (or became a leader), unregister from the cluster and update the registry
    @Override
    public void unRegisterLeaderFromCluster() throws KeeperException, InterruptedException {
        dataCenterRegistry.unregisterFromCluster();
        dataCenterRegistry.updateDataCenterRegistry();
    }

    // If node is a follower, register to cluster to become available in the pool
    @Override
    public void registerFollowerToCluster() {
        try {
            String currentServerAddress =
                    String.format("http://%s:%d", InetAddress.getLocalHost().getCanonicalHostName(), port);

            dataCenterRegistry.registerToCluster(currentServerAddress);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }
}
