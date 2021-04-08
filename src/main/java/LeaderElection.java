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

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final String ELECTION_NAMESPACE = "/election";
    private static final int SESSION_TIMEOUT = 3000;

    private ZooKeeper zooKeeper;
    private String currentZnodeName;


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

        System.out.println("znode name " + znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace("/election/", "");
    }

    // Electing the leader - Can be re-elected by calling whenever a node goes down
    public void electLeader() throws KeeperException, InterruptedException {
        List<String> replicas = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
        Stat leaderExists = null;
        String leader = null;

        // Main leader election logic - Get the smallest replica number
        while (leaderExists == null) {
            Collections.sort(replicas);
            leader = replicas.get(0);

            // Detecting the leader database
            if (leader.equals(currentZnodeName)) {
                System.out.println("I am the leader");
                return;
            } else {
                // Followers announce the leader that they are watching
                System.out.println("I am a follower");
                leaderExists = zooKeeper.exists(ELECTION_NAMESPACE + "/" + leader, this);
                if (leaderExists == null) {
                    System.out.println("Leader has died. Volunteering to be leader...");
                }
            }
        }

        System.out.println("Watching leader: " + leader + "...");
    }

    // This is the method where we watch for events from Zookeeper,
    // including node connection success, failures, etc
    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None: {
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Success: Connected to zookeeper");
                } else {
                    synchronized (this.zooKeeper) {
                        System.out.println("Disconnected from zookeeper event");
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
        }
    }

    private void connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this) ;
    }

    // The main method to set the stage
    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {

        //Begin with creating replicas and electing a leader
        LeaderElection leaderElection = new LeaderElection();
        leaderElection.connectToZookeeper();
        leaderElection.volunteerForLeadership();
        leaderElection.electLeader();

        // Now we need to wait on background threads for zookeeper to do its job
        // Zookeeper will notifyALL later to us
        leaderElection.run();
        leaderElection.close();
    }

    private void close() throws InterruptedException {
        this.zooKeeper.close();
    }
}
