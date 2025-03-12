package tests.text_io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class text_io {
    public static void main(String[] args) throws IOException {
        String load_path = "../../json/random" + args[0] + ".json";
        String store_path = "../../json/java_out_" + args[0] + ".txt";

        String content = Files.readString(Paths.get(load_path));
        Files.writeString(Paths.get(store_path), content);
    }
}
