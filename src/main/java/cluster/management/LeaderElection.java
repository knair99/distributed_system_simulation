package cluster.management;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {

    // config.Configuration data
    private static final String ELECTION_NAMESPACE = "/election";
    private final String failOverMethod;
    private final ReplicaRegistrationHelper replicaRegistrationHelper;

    private ZooKeeper zooKeeper;

    // Current node information
    private String currentReplicaName;


    public LeaderElection(String failOverMethod, ZooKeeper zooKeeper, ReplicaRegistrationHelper replicaRegistrationHelper) {
        this.failOverMethod = failOverMethod;
        this.zooKeeper = zooKeeper;
        this.replicaRegistrationHelper = replicaRegistrationHelper;
    }


    // This method is called every time our process (database replica) registers
    public void volunteerForLeadership() throws KeeperException, InterruptedException {
        String znodePrefix = ELECTION_NAMESPACE + "/replica_";
        // TODO: Need to figure out a way to store data in bytes later
        String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("Replica name " + znodeFullPath);
        this.currentReplicaName = znodeFullPath.replace("/election/", "");
    }

    // Electing the leader - Can be re-elected by calling whenever a node goes down
    public void electLeader() throws KeeperException, InterruptedException {
        List<String> replicas = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
        Stat watchedNodeExists = null;
        String leaderNode = null;

        // Main leader election logic - Get the smallest replica number
        while (watchedNodeExists == null) {
            // Sort replicas by id
            Collections.sort(replicas);
            leaderNode = replicas.get(0);

            // Detecting the leader database
            if (leaderNode.equals(currentReplicaName)) {
                System.out.println("I am the leader replica");
                replicaRegistrationHelper.unRegisterLeaderFromCluster();
                return;
            } else {
                // Followers announce the leader that they are watching
                System.out.println("I am a follower replica");
                watchedNodeExists = maybeFailOver(replicas);
                replicaRegistrationHelper.registerFollowerToCluster();
            }
        }

    }

    // The watched node can be the leader or each follower watching its predecessor
    private Stat maybeFailOver(List<String> replicas) throws KeeperException, InterruptedException {
        String leaderNode = replicas.get(0);
        Stat watchedNodeExists = null;
        String watchedNodeName = null;

        if (this.failOverMethod.equals("watch_leader")) {
            watchedNodeExists = zooKeeper.exists(ELECTION_NAMESPACE + "/" + leaderNode, this);
            watchedNodeName = leaderNode;
            return watchedNodeExists;
        } else if (this.failOverMethod.equals("watch_circle")) {
            int predecessorIndex = Collections.binarySearch(replicas, this.currentReplicaName) - 1;
            String predecessorNodeName = replicas.get(predecessorIndex);
            watchedNodeName = predecessorNodeName;
            watchedNodeExists = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorNodeName, this);
        }

        System.out.println("Watching Node: " + watchedNodeName + "...");
        return watchedNodeExists;
    }

    // This is the method where we watch for events from Zookeeper,
    // including node connection success, failures, etc
    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case NodeDeleted: {
                // In the event a leader replica goes down, elect a new leader
                try {
                    electLeader();
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
            default:
                throw new IllegalStateException("Unexpected event: " + watchedEvent.getType());
        }
    }


}
