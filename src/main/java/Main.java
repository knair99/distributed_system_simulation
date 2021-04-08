import org.apache.zookeeper.KeeperException;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public class Main {
    public LeaderElection leaderElection;

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException, ParseException {
        // Begin by getting the configuration
        Configuration configuration = new Configuration();
        String failOverMethod = configuration.getFailOverMethod();
        System.out.println("Failover method chosen: " + failOverMethod);

        // Now register replicas with config setting for watching nodes
        registerReplicas(failOverMethod);
    }

    private static void registerReplicas(String failOverMethod) throws IOException, KeeperException, InterruptedException {
        // First connect to zookeeper
        LeaderElection leaderElection = new LeaderElection(failOverMethod);
        leaderElection.connectToZookeeper();

        // Begin with creating replicas and electing a leader
        leaderElection.volunteerForLeadership();
        leaderElection.electLeader();

        // Now we need to wait on background threads for zookeeper to do its job
        // Zookeeper will notifyALL later to us
        leaderElection.run();
        leaderElection.close();
    }

}
