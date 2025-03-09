import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class text_load_store {
    public static void main(String[] args) throws IOException {
        String content = Files.readString(Paths.get("timeseries.json"));
        Files.writeString(Paths.get("java_out.txt"), content);
    }
}
