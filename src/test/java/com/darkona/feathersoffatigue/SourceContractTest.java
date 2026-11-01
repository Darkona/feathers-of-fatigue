package com.darkona.feathersoffatigue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceContractTest {

    private enum Comment { LINE, BLOCK, JAVADOC }

    private static List<Comment> comments(String source) {
        List<Comment> found = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (source.startsWith("\"\"\"", i)) {
                i = source.indexOf("\"\"\"", i + 3) + 3;
            } else if (c == '"' || c == '\'') {
                i++;
                while (source.charAt(i) != c) i += source.charAt(i) == '\\' ? 2 : 1;
                i++;
            } else if (source.startsWith("//", i)) {
                found.add(Comment.LINE);
                i = source.indexOf('\n', i);
                if (i < 0) break;
            } else if (source.startsWith("/*", i)) {
                found.add(source.startsWith("/**", i) ? Comment.JAVADOC : Comment.BLOCK);
                i = source.indexOf("*/", i) + 2;
            } else {
                i++;
            }
        }
        return found;
    }

    private static List<String> offenders(String root, List<Comment> forbidden) throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(root))) {
            return files.filter(path -> path.toString().endsWith(".java")).filter(path -> {
                try {
                    return comments(Files.readString(path)).stream().anyMatch(forbidden::contains);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }).map(Path::toString).toList();
        }
    }

    @Test
    void noCommentsInThePublicRepository() throws IOException {
        List<Comment> all = List.of(Comment.values());
        assertEquals(List.of(), offenders("src/main/java", all));
        assertEquals(List.of(), offenders("src/test/java", all));
        assertEquals(List.of(), offenders("src/gametest/java", all));
    }

    @Test
    void theApiHasOnlyJavadoc() throws IOException {
        assertEquals(List.of(), offenders("src/api/java", List.of(Comment.LINE, Comment.BLOCK)));
    }
}
