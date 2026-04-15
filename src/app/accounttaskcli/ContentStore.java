package app.accounttaskcli;

import java.io.IOException;
import java.util.Optional;

public interface ContentStore {
    String put(String content) throws IOException;

    Optional<String> get(String hash) throws IOException;
}
