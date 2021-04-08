import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;

public class Configuration {

    public JSONObject config;

    public Configuration() throws IOException, ParseException {
        this.config = getConfig();
    }

    @SuppressWarnings("unchecked")
    public JSONObject getConfig() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();

        FileReader reader = new FileReader("/Users/kprasad/Dropbox/Focus/DIST/kublai/src/main/resources/config.json");
        Object obj = jsonParser.parse(reader);
        config = (JSONObject) obj;
        return config;

    }

    public String getFailOverMethod() {
        String failOverMethod = (String) this.config.get("failOverMethod");
        return failOverMethod;
    }
}
