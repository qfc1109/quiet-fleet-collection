package com.qfc.file;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileArchive {

    private final String originalName;
    private final String mimeType;
    private final List<Entry> entries;

    public FileArchive(String originalName, String mimeType, List<Entry> entries) {
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static class Entry {
        private final String name;
        private final Path path;

        public Entry(String name, Path path) {
            this.name = name;
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public Path getPath() {
            return path;
        }
    }
}
