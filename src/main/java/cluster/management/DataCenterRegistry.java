package cluster.management;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataCenterRegistry implements Watcher {
    private static final String DATA_CENTER_REGISTRY_NODE = "/data_center_registry";

    private static ZooKeeper zooKeeper;
    private static DataCenterRegistry dataCenterRegistryInstance = null;
    private String currentReplicaInfoNode = null;
    private List<String> allRegisteredReplicaAddresses = null;

    public static DataCenterRegistry getInstance() {
        if (dataCenterRegistryInstance == null) {
            dataCenterRegistryInstance = new DataCenterRegistry();
            return dataCenterRegistryInstance;
        }
        return dataCenterRegistryInstance;
    }

    // First create the data center registry znode (persistent)
    private static void createDataCenterRegistry() {
        try {
            if (zooKeeper.exists(DATA_CENTER_REGISTRY_NODE, false) == null) {
                zooKeeper.create(DATA_CENTER_REGISTRY_NODE, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void init(ZooKeeper zooKeeper) {
        DataCenterRegistry.zooKeeper = zooKeeper;
        createDataCenterRegistry();
    }

    // Called whenever a follower node needs to join a cluster
    public void registerToCluster(String metadata) throws KeeperException, InterruptedException {
        if (this.currentReplicaInfoNode != null) {
            System.out.println("Already registered to data center registry");
            return;
        }
        this.currentReplicaInfoNode = zooKeeper.create(DATA_CENTER_REGISTRY_NODE + "/n_", metadata.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Registered to data center registry");
    }

    // Typically called when a leader node wants to unregister from a cluster
    public void unregisterFromCluster() {
        try {
            if (currentReplicaInfoNode != null && zooKeeper.exists(currentReplicaInfoNode, false) != null) {
                zooKeeper.delete(currentReplicaInfoNode, -1);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    // Gets current state of the cluster - all leader/follower replicas information
    public synchronized List<String> getAllRegisteredReplicaAddresses() throws KeeperException, InterruptedException {
        if (allRegisteredReplicaAddresses == null) {
            updateDataCenterRegistry();
        }
        return allRegisteredReplicaAddresses;
    }


    // Synchronized method to update the data center registry -
    // Can be called when a node becomes a leader to unregister from cluster
    // Or when a node becomes a follower and registers to the cluster
    // Or if all nodes have died in the cluster and the one node comes back on (as leader)
    public synchronized void updateDataCenterRegistry() throws KeeperException, InterruptedException {
        List<String> replicaInfoList = zooKeeper.getChildren(DATA_CENTER_REGISTRY_NODE, this);

        List<String> replicaAddresses = new ArrayList<>(replicaInfoList.size());

        for (String replicaInfo : replicaInfoList) {
            String replicaFullPath = DATA_CENTER_REGISTRY_NODE + "/" + replicaInfo;
            Stat stat = zooKeeper.exists(replicaFullPath, false);
            if (stat == null) {
                continue;
            }

            byte[] replicaAddressBytes = zooKeeper.getData(replicaFullPath, false, stat);
            String replicaAddress = new String(replicaAddressBytes);
            replicaAddresses.add(replicaAddress);
        }

        this.allRegisteredReplicaAddresses = Collections.unmodifiableList(replicaAddresses);
        System.out.println("The replicas addresses are: " + this.allRegisteredReplicaAddresses);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        try {
            updateDataCenterRegistry();
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
