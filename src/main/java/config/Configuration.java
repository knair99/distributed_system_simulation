package config;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;

public class Configuration {

    public static boolean isLeader = false;
    public static int port;
    public static JSONObject config;
    private static Configuration configuration;

    public static int getPort() {
        return port;
    }

    public static void setPort(int port) {
        Configuration.port = port;
    }

    public static Configuration getInstance() {
        if (configuration == null) {
            configuration = new Configuration();
            try {
                config = getConfig();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return configuration;
    }

    public static boolean isIsLeader() {
        return isLeader;
    }

    public static void setIsLeader(boolean isLeader) {
        Configuration.isLeader = isLeader;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getConfig() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();

        FileReader reader = new FileReader("/Users/kprasad/Dropbox/Focus/DIST/kublai/src/main/resources/config.json");
        Object obj = jsonParser.parse(reader);
        config = (JSONObject) obj;
        return config;

    }

    private void init() throws IOException, ParseException {
    }

    public String getFailOverMethod() {
        String failOverMethod = (String) config.get("failOverMethod");
        return failOverMethod;
    }

    public String getReplicationStrategy() {
        String replicationStrategy = (String) config.get("replicationStrategy");
        return replicationStrategy;
    }
}
