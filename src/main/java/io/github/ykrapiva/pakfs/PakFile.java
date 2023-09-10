package io.github.ykrapiva.pakfs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
class PakFile {
    @Getter
    private final Path path;
    private volatile Map<String, PakFileEntry> fileEntries;

    Map<String, PakFileEntry> getEntries() throws IOException {
        if (fileEntries == null) {
            synchronized (this) {
                if (fileEntries == null) {
                    try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
                        fileEntries = readEntries(fileChannel).stream()
                                .collect(Collectors.toMap(PakFileEntry::getName, Function.identity()));
                    }
                }
            }

        }
        return fileEntries;
    }

    @Nonnull
    SeekableByteChannel newReadOnlyChannel(@Nonnull PakFileEntry fileEntry) throws IOException {
        FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
        return new PakFileEntryChannel(fileChannel, fileEntry);
    }

    @Nonnull
    private Collection<PakFileEntry> readEntries(@Nonnull FileChannel fileChannel) throws IOException {
        final String id = readString(fileChannel, 4);

        if (!"PACK".equals(id)) {
            throw new FileFormatException("Unexpected file identifier: " + id);
        }

        final int fileTableOffset = readInt(fileChannel);
        final int fileTableSize = readInt(fileChannel);

        final int numFileEntries = fileTableSize / 64;

        if (numFileEntries < 0) {
            throw new FileFormatException("Unexpected number of file entries: " + numFileEntries);
        }

        fileChannel.position(fileTableOffset);

        List<PakFileEntry> fileEntries = new ArrayList<>(numFileEntries);

        for (int i = 0; i < numFileEntries; i++) {
            String fileName = readString(fileChannel, 56);
            int fileOffset = readInt(fileChannel);
            int fileSize = readInt(fileChannel);

            fileEntries.add(new PakFileEntry(fileName, fileOffset, fileSize));
        }

        return fileEntries;
    }

    @Nonnull
    private String readString(@Nonnull FileChannel fileChannel, int size) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        fileChannel.read(buffer);
        return new String(buffer.array(), StandardCharsets.UTF_8).trim();
    }

    private int readInt(@Nonnull FileChannel fileChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        fileChannel.read(buffer);
        return buffer.getInt(0);
    }
}
