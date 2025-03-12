import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;

public class json_load_store {
    public static void main(String[] args) throws IOException {
        String load_path = "random" + args[0] + ".json";
        String store_path = "java_out_" + args[0] + ".json";

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(new File(load_path));
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(store_path), jsonNode);
    }
}
