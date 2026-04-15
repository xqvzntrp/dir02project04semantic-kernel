package app.accounttaskcli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

public final class FileContentStore implements ContentStore {

    private final Path root;

    public FileContentStore(Path root) {
        this.root = root;
    }

    @Override
    public String put(String content) throws IOException {
        String hash = sha256(content);
        Path path = pathFor(hash);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        if (!Files.exists(path)) {
            Files.writeString(path, content);
        }
        return hash;
    }

    @Override
    public Optional<String> get(String hash) throws IOException {
        Path path = pathFor(hash);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(Files.readString(path));
    }

    private Path pathFor(String hash) {
        if (hash.length() < 4) {
            throw new IllegalArgumentException("hash must be at least 4 characters");
        }
        return root.resolve(hash.substring(0, 2)).resolve(hash.substring(2, 4)).resolve(hash);
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
