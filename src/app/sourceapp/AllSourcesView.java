package app.sourceapp;

import java.nio.file.Path;

public record AllSourcesView(
    Path sourcePath,
    String content
) {
    public AllSourcesView {
        if (sourcePath == null) {
            throw new IllegalArgumentException("sourcePath must not be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
    }
}
