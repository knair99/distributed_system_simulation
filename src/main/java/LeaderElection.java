import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {

    // Configuration data
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final String ELECTION_NAMESPACE = "/election";
    private static final int SESSION_TIMEOUT = 3000;
    private final String failOverMethod;

    private ZooKeeper zooKeeper;

    // Current node information
    private String currentReplicaName;


    public LeaderElection(String failOverMethod) {
        this.failOverMethod = failOverMethod;
    }


    // Required to wait around till Zookeeper's worker threads finish their job
    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            this.zooKeeper.wait();
        }
    }

    // This method is called every time our process (database) registers
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
                System.out.println("I am the leader");
                return;
            } else {
                // Followers announce the leader that they are watching
                System.out.println("I am a follower");
                watchedNodeExists = maybeFailOver(replicas);
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
            case None: {
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Success: Connected to zookeeper!");
                } else {
                    synchronized (this.zooKeeper) {
                        System.out.println("Disconnected from zookeeper!");
                        this.zooKeeper.notifyAll();
                    }
                }
                break;
            }
            case NodeCreated:
                break;
            case NodeDeleted: {
                // In the event a leader replica goes down, elect a new leader
                try {
                    electLeader();
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            case NodeDataChanged:
                break;
            case NodeChildrenChanged:
                break;
            case DataWatchRemoved:
                break;
            case ChildWatchRemoved:
                break;
            case PersistentWatchRemoved:
                break;
            default:
                throw new IllegalStateException("Unexpected event: " + watchedEvent.getType());
        }
    }

    public void connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    public void close() throws InterruptedException {
        this.zooKeeper.close();
    }
}
