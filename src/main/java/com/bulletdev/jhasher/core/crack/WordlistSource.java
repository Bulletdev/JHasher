package com.bulletdev.jhasher.core.crack;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Lazy wordlist reader that never loads the entire file into memory.
 * Supports file-based and classpath-based (bundled) sources.
 */
public final class WordlistSource implements Iterable<String>, AutoCloseable {

    private final Path filePath;
    private final InputStream stream;
    private final List<String> inMemoryList;
    private BufferedReader activeReader;

    private WordlistSource(Path filePath, InputStream stream, List<String> inMemoryList) {
        this.filePath = filePath;
        this.stream = stream;
        this.inMemoryList = inMemoryList;
    }

    /**
     * Create a wordlist from a file on disk.
     */
    public static WordlistSource fromFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Wordlist file not found: " + path);
        }
        return new WordlistSource(path, null, null);
    }

    /**
     * Create a wordlist from a bundled classpath resource.
     * <p>
     * Known names:
     * <ul>
     *   <li>"top1000" - loads data/top-passwords.json (JSON array)</li>
     * </ul>
     * Any other name is tried as a classpath resource path.
     */
    public static WordlistSource bundled(String name) {
        if ("top1000".equals(name)) {
            InputStream is = WordlistSource.class.getClassLoader()
                    .getResourceAsStream("data/top-passwords.json");
            if (is == null) {
                throw new IllegalStateException("Bundled resource data/top-passwords.json not found on classpath");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                Type listType = new TypeToken<List<String>>() {}.getType();
                List<String> passwords = new Gson().fromJson(reader, listType);
                return new WordlistSource(null, null, passwords);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read bundled wordlist", e);
            }
        }

        // Try as a generic classpath resource (line-based)
        InputStream is = WordlistSource.class.getClassLoader().getResourceAsStream(name);
        if (is == null) {
            throw new IllegalArgumentException("Classpath resource not found: " + name);
        }
        return new WordlistSource(null, is, null);
    }

    /**
     * Estimate the number of words in this source.
     * Returns -1 if the size cannot be estimated cheaply.
     */
    public long estimateSize() {
        if (inMemoryList != null) {
            return inMemoryList.size();
        }
        if (filePath != null) {
            try (BufferedReader br = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                long count = 0;
                while (br.readLine() != null) {
                    count++;
                }
                return count;
            } catch (IOException e) {
                return -1;
            }
        }
        return -1;
    }

    @Override
    public Iterator<String> iterator() {
        if (inMemoryList != null) {
            return inMemoryList.iterator();
        }

        if (filePath != null) {
            try {
                BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
                activeReader = reader;
                return new LineIterator(reader);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to open wordlist: " + filePath, e);
            }
        }

        if (stream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            activeReader = reader;
            return new LineIterator(reader);
        }

        return List.<String>of().iterator();
    }

    @Override
    public void close() {
        if (activeReader != null) {
            try {
                activeReader.close();
            } catch (IOException ignored) {
                // best-effort close
            }
            activeReader = null;
        }
    }

    /**
     * Iterator that reads lines lazily from a BufferedReader.
     */
    private static final class LineIterator implements Iterator<String> {

        private final BufferedReader reader;
        private String nextLine;
        private boolean finished;

        LineIterator(BufferedReader reader) {
            this.reader = reader;
            advance();
        }

        private void advance() {
            try {
                nextLine = reader.readLine();
                if (nextLine == null) {
                    finished = true;
                    reader.close();
                }
            } catch (IOException e) {
                finished = true;
                try {
                    reader.close();
                } catch (IOException ignored) {
                    // best-effort close
                }
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public boolean hasNext() {
            return !finished;
        }

        @Override
        public String next() {
            if (finished) {
                throw new NoSuchElementException("No more words in wordlist");
            }
            String current = nextLine;
            advance();
            return current;
        }
    }
}
