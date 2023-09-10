package io.github.ykrapiva.pakfs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PakFileSystemTest {
    @Mock
    private PakFileSystemProvider provider;
    @TempDir
    private Path tempDir;
    private PakFileSystem subject;

    @BeforeEach
    void setUp() throws IOException {
        Path testPak = tempDir.resolve("test.pak");

        String[] testPakEntries = {
                "maps/level1.bsp",
                "maps/level2.bsp",
                "textures/texture.tga",
                "palette.pcx"
        };

        PakFileCreator.createPakFile(testPak, testPakEntries);

        URI testPakUri = URI.create("file:" + testPak);

        subject = new PakFileSystem(provider, testPakUri);
    }

    @AfterEach
    void tearDown() {
        if (subject != null) {
            subject.close();
        }
    }

    @Test
    void provider() {
        assertThat(subject.provider).isSameAs(provider);
    }

    @Test
    void nonCloseable() {
        assertThat(subject.isOpen()).isTrue();
        subject.close();
        assertThat(subject.isOpen()).isTrue();
    }

    @Test
    void isReadOnly() {
        assertThat(subject.isReadOnly()).isTrue();
    }

    @Test
    void getSeparator() {
        assertThat(subject.getSeparator()).isEqualTo("/");
    }

    @Test
    void getRootDirectories() {
        List<Path> rootDirectories = StreamSupport.stream(subject.getRootDirectories().spliterator(), false).collect(Collectors.toList());
        assertThat(rootDirectories).containsExactly(new PakPath(subject, "/"));
    }

    @Test
    void getFileStores() {
        assertThat(subject.getFileStores()).isEmpty();
    }

    @Test
    void supportedFileAttributeViews() {
        assertThat(subject.supportedFileAttributeViews()).containsExactly("basic");
    }

    @Test
    void getPath() {
        assertThat(subject.getPath("").toString()).isEqualTo("/");
        assertThat(subject.getPath("world").toString()).isEqualTo("world");
        assertThat(subject.getPath("world", "maps", "level1.bsp").toString()).isEqualTo("world/maps/level1.bsp");
    }

    @Test
    void getPathMatcher() {
        assertThrows(UnsupportedOperationException.class, () -> subject.getPathMatcher("maps"));
    }

    @Test
    void getUserPrincipalLookupService() {
        assertThrows(UnsupportedOperationException.class, () -> subject.getUserPrincipalLookupService());
    }

    @Test
    void newWatchService() {
        assertThrows(UnsupportedOperationException.class, () -> subject.newWatchService());
    }

    @Test
    void iterator() {
        List<Path> subjectIteratorElements = new ArrayList<>();
        subject.iterator(path -> path.toString().startsWith("maps"))
                .forEachRemaining(subjectIteratorElements::add);

        assertThat(subjectIteratorElements).allSatisfy(path -> assertThat(path).isInstanceOf(PakPath.class));
        assertThat(subjectIteratorElements).map(Path::toString).containsOnly(
                "maps/level1.bsp",
                "maps/level2.bsp"
        );
    }
}