package io.github.ykrapiva.pakfs;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class PakFileSystemIntegrationTest {
    @TempDir
    private Path tempDir;

    private Path testPakPath;
    private PakPath rootPath;
    private String[] testPakEntries;

    @BeforeEach
    void setUp() throws IOException {
        testPakPath = tempDir.resolve("test.pak");

        testPakEntries = new String[]{
                "maps/level1.bsp",
                "maps/level2.bsp",
                "textures/texture.tga",
                "palette.pcx"
        };

        PakFileCreator.createPakFile(testPakPath, testPakEntries);

        rootPath = PakPath.get(testPakPath);
    }

    @SuppressWarnings("resource")
    @Test
    void test() throws Exception {
        assertThat(rootPath).isInstanceOf(PakPath.class);
        assertThat(Files.isDirectory(rootPath)).isTrue();
        assertThat(Files.isRegularFile(rootPath)).isFalse();

        List<String> entries = StreamSupport.stream(Files.newDirectoryStream(rootPath).spliterator(), false)
                .map(Path::toString)
                .collect(Collectors.toList());
        assertThat(entries).containsOnly(testPakEntries);

        Path pakEntryPath1 = rootPath.resolve("maps/level1.bsp");
        assertThat(pakEntryPath1).isInstanceOf(PakPath.class);

        try (InputStream is = Files.newInputStream(pakEntryPath1)) {
            assertThat(IOUtils.toString(is, StandardCharsets.UTF_8))
                    .isEqualTo("maps/level1.bsp");
        }

        URI uri = URI.create("pak:" + testPakPath.toAbsolutePath() + "!maps/level2.bsp");
        Path pakEntryPath2 = Paths.get(uri);
        assertThat(pakEntryPath2).isInstanceOf(PakPath.class);

        for (int i = 0; i < 5; i++) {
            try (InputStream is = Files.newInputStream(pakEntryPath2)) {
                assertThat(IOUtils.toString(is, StandardCharsets.UTF_8))
                        .isEqualTo("maps/level2.bsp");
            }
        }
    }

    @Test
    void reading() throws IOException {
        Path path = rootPath.resolve("palette.pcx");
        try (SeekableByteChannel channel = Files.newByteChannel(path)) {
            ByteBuffer buffer1 = ByteBuffer.allocate(4);
            ByteBuffer buffer2 = ByteBuffer.allocate(7);
            channel.read(buffer1);
            channel.read(buffer2);
            String s1 = new String(buffer1.array(), StandardCharsets.UTF_8);
            String s2 = new String(buffer2.array(), StandardCharsets.UTF_8);
            assertThat(s1 + s2).isEqualTo("palette.pcx");
        }
    }
}
