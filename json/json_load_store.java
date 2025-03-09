import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;

public class json_load_store {
    public static void main(String[] args) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        // load data
        JsonNode jsonNode = objectMapper.readTree(new File("timeseries.json"));

        // save data
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("java_out.json"), jsonNode);
    }
}
