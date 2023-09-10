package io.github.ykrapiva.pakfs;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PakFileSystemProviderTest {
    @TempDir
    private Path tempDir;
    private URI testPakUri;
    private final PakFileSystemProvider subject = new PakFileSystemProvider();

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

        testPakUri = URI.create("pak:" + testPak);
    }

    @Test
    void getScheme() {
        assertThat(subject.getScheme()).isEqualTo("pak");
    }

    @Test
    @SuppressWarnings("resource")
    void newFileSystem() {
        PakFileSystem fileSystem = subject.newFileSystem(testPakUri, Collections.emptyMap());
        assertThat(fileSystem).isNotNull();
        assertThrows(FileSystemAlreadyExistsException.class, () -> subject.newFileSystem(testPakUri, Collections.emptyMap()));
    }

    @Test
    void getFileSystem() {
        assertThrows(FileSystemNotFoundException.class, () -> subject.getFileSystem(testPakUri));

        PakFileSystem fileSystem = subject.newFileSystem(testPakUri, Collections.emptyMap());
        assertThat(subject.getFileSystem(testPakUri)).isSameAs(fileSystem);
    }

    @Test
    void getOrCreateFileSystem() throws IOException {
        PakFileSystem fileSystem1 = subject.getOrCreateFileSystem(testPakUri);
        PakFileSystem fileSystem2 = subject.getOrCreateFileSystem(testPakUri);
        PakFileSystem fileSystem3 = subject.getOrCreateFileSystem(URI.create(testPakUri.toString() + "!map/level.bsp"));
        assertThat(fileSystem1).isNotNull();
        assertThat(fileSystem2).isSameAs(fileSystem1);
        assertThat(fileSystem3).isSameAs(fileSystem1);
    }

    @Test
    void getPath() {
        URI uri = URI.create(testPakUri.toString() + "!map/level.bsp");
        Path path = subject.getPath(uri);
        assertThat(path).isInstanceOf(PakPath.class);
        assertThat(path.toUri()).isEqualTo(uri);
    }

    @Test
    void newByteChannel() throws IOException {
        Path path = Paths.get(URI.create(testPakUri + "!maps/level1.bsp"));
        try (SeekableByteChannel channel = subject.newByteChannel(path, Collections.emptySet(), (FileAttribute<?>[]) null)) {
            Reader reader = Channels.newReader(channel, StandardCharsets.UTF_8.newDecoder(), -1);
            assertThat(IOUtils.toString(reader)).isEqualTo("maps/level1.bsp");
        }
    }

    @Test
    @SuppressWarnings("resource")
    void newByteChannel_whenNonSingleReadOptionSpecified_returnsUnsupportedOperationException() {
        Path path = Paths.get(URI.create(testPakUri + "!maps/level1.bsp"));
        assertThrows(UnsupportedOperationException.class, () -> subject.newByteChannel(path, new HashSet<>(Arrays.asList(READ, WRITE)), (FileAttribute<?>[]) null));
    }

    @Test
    @SuppressWarnings("resource")
    void newDirectoryStream() throws IOException {
        Path rootPath = Paths.get(testPakUri);
        Path entryPath = Paths.get(URI.create(testPakUri + "!maps/level1.bsp"));

        List<Path> pakFileEntries = StreamSupport.stream(subject.newDirectoryStream(rootPath, path -> true).spliterator(), false).collect(Collectors.toList());
        assertThat(pakFileEntries).containsOnly(
                Paths.get(URI.create(testPakUri.toString() + "!maps/level1.bsp")),
                Paths.get(URI.create(testPakUri.toString() + "!maps/level2.bsp")),
                Paths.get(URI.create(testPakUri.toString() + "!textures/texture.tga")),
                Paths.get(URI.create(testPakUri.toString() + "!palette.pcx"))
        );

        assertThrows(NotDirectoryException.class, () -> subject.newDirectoryStream(entryPath, path -> true));
    }

    @Test
    void createDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> subject.createDirectory(Paths.get("/dir")));
    }

    @Test
    void delete() {
        assertThrows(UnsupportedOperationException.class, () -> subject.delete(Paths.get("/dir")));
    }

    @Test
    void copy() {
        assertThrows(UnsupportedOperationException.class, () -> subject.copy(Paths.get("/from"), Paths.get("/to")));
    }

    @Test
    void move() {
        assertThrows(UnsupportedOperationException.class, () -> subject.move(Paths.get("/from"), Paths.get("/to")));
    }

    @Test
    void isSameFile() {
        Path path1 = Paths.get(URI.create(testPakUri.toString() + "!maps/level1.bsp"));
        Path path2 = Paths.get(URI.create(testPakUri.toString() + "!maps/level1.bsp"));
        Path path3 = Paths.get(URI.create(testPakUri.toString() + "!maps/level2.bsp"));

        assertThat(subject.isSameFile(path1, path2)).isTrue();
        assertThat(subject.isSameFile(path1, path3)).isFalse();
    }

    @Test
    void isHidden() {
        assertThat(subject.isHidden(Paths.get("/file"))).isFalse();
    }

    @Test
    void checkAccess() throws Exception {
        Path validPath = Paths.get(URI.create(testPakUri.toString() + "!maps/level1.bsp"));
        Path invalidPath = Paths.get(URI.create(testPakUri.toString() + "!non-existent-file.bsp"));

        subject.checkAccess(validPath);
        subject.checkAccess(validPath, AccessMode.READ);
        assertThrows(NoSuchFileException.class, () -> subject.checkAccess(invalidPath));
        assertThrows(AccessDeniedException.class, () -> subject.checkAccess(validPath, AccessMode.WRITE));
        assertThrows(AccessDeniedException.class, () -> subject.checkAccess(validPath, AccessMode.EXECUTE));
        assertThrows(AccessDeniedException.class, () -> subject.checkAccess(validPath, AccessMode.READ, AccessMode.WRITE));
        assertThrows(AccessDeniedException.class, () -> subject.checkAccess(invalidPath, AccessMode.WRITE));
        assertThrows(AccessDeniedException.class, () -> subject.checkAccess(invalidPath, AccessMode.EXECUTE));
    }

    @Test
    void getFileAttributeView() {
        assertThat(subject.getFileAttributeView(tempDir, FileAttributeView.class, (LinkOption[]) null)).isNull();
    }

    @Test
    void readAttributes() throws Exception {
        Path rootPath = Paths.get(testPakUri);
        Path validPath = Paths.get(URI.create(testPakUri.toString() + "!maps/level1.bsp"));
        Path invalidPath = Paths.get(URI.create(testPakUri.toString() + "!non-existent-file.bsp"));

        PakPathAttributes attributes = subject.readAttributes(rootPath, PakPathAttributes.class, LinkOption.NOFOLLOW_LINKS);

        assertThat(attributes).isNotNull();
        assertThat(attributes.isDirectory()).isTrue();
        assertThat(attributes.isRegularFile()).isFalse();
        assertThat(attributes.size()).isEqualTo(0);
        assertThat(attributes.lastModifiedTime()).isNull();
        assertThat(attributes.lastAccessTime()).isNull();
        assertThat(attributes.creationTime()).isNull();
        assertThat(attributes.isSymbolicLink()).isFalse();
        assertThat(attributes.isOther()).isFalse();
        assertThat(attributes.fileKey()).isNull();

        attributes = subject.readAttributes(validPath, PakPathAttributes.class, LinkOption.NOFOLLOW_LINKS);

        assertThat(attributes).isNotNull();
        assertThat(attributes.isDirectory()).isFalse();
        assertThat(attributes.isRegularFile()).isTrue();
        assertThat(attributes.size()).isEqualTo(15);
        assertThat(attributes.lastModifiedTime()).isNull();
        assertThat(attributes.lastAccessTime()).isNull();
        assertThat(attributes.creationTime()).isNull();
        assertThat(attributes.isSymbolicLink()).isFalse();
        assertThat(attributes.isOther()).isFalse();
        assertThat(attributes.fileKey()).isNull();

        assertThrows(NoSuchFileException.class, () -> subject.readAttributes(invalidPath, PakPathAttributes.class, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    void readAttributesAsMap() {
        Path path = Paths.get(testPakUri);
        Map<String, Object> attributes = subject.readAttributes(path, (String) null, LinkOption.NOFOLLOW_LINKS);
        assertThat(attributes).isEmpty();
    }

    @Test
    void setAttribute() {
        Path path = Paths.get(testPakUri);
        assertThrows(UnsupportedOperationException.class, () -> subject.setAttribute(path, "attr", "value"));
    }
}