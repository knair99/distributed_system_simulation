import cluster.management.DataCenterRegistry;
import cluster.management.LeaderElection;
import cluster.management.ReplicaRegistrationHelper;
import cluster.management.ReplicaRegistrationHelperImpl;
import config.Configuration;
import networking.WebServer;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public class Application implements Watcher {

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;

    private static LeaderElection leaderElection;
    private static ZooKeeper zooKeeper = null;
    private static int DEFAULT_PORT = 9081;

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException, ParseException {
        Application application = new Application();
        zooKeeper = application.connectToZookeeper();

        // Begin by getting the configuration
        Configuration configuration = Configuration.getInstance();
        String failOverMethod = configuration.getFailOverMethod();
        System.out.println("Failover method chosen: " + failOverMethod);

        // Now register replicas with config setting for watching nodes
        int port = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        registerReplicas(port);


        // Now, setup the webserver to listen on the same port
        WebServer webServer = new WebServer(port);
        webServer.startServer();

        // Now run the application
        application.run();
        application.close();
    }

    private void close() throws InterruptedException {
        this.zooKeeper.close();
    }

    // Now we need to wait on background threads for zookeeper to do its job
    // Zookeeper will notifyALL later to us
    private void run() throws InterruptedException {
        synchronized (zooKeeper) {
            this.zooKeeper.wait();
        }
    }

    private static void registerReplicas(int port) throws IOException, KeeperException,
            InterruptedException {
        // First prepare enough information for replicas to register to the data center
        DataCenterRegistry dataCenterRegistry = DataCenterRegistry.getInstance();
        dataCenterRegistry.init(zooKeeper);

        ReplicaRegistrationHelper replicaRegistrationHelper = new ReplicaRegistrationHelperImpl(dataCenterRegistry, port);

        // Then connect to zookeeper
        leaderElection = new LeaderElection(zooKeeper, replicaRegistrationHelper);

        // Begin with creating replicas and electing a leader
        leaderElection.volunteerForLeadership();
        leaderElection.electLeader();

    }

    public ZooKeeper connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
        return this.zooKeeper;
    }

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
            default:
                throw new IllegalStateException("Unexpected event: " + watchedEvent.getType());
        }
    }
}
