package app.sourceapp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AllSourcesViews {

    private static final Path ALL_SOURCES_PATH = Path.of("all-sources.txt");

    private AllSourcesViews() {
    }

    public static AllSourcesView load() {
        try {
            return new AllSourcesView(ALL_SOURCES_PATH, Files.readString(ALL_SOURCES_PATH));
        } catch (IOException e) {
            throw new IllegalStateException("failed to read all sources file: " + ALL_SOURCES_PATH, e);
        }
    }
}
