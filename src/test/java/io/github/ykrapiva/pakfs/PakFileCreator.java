package io.github.ykrapiva.pakfs;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

import static java.nio.file.StandardOpenOption.WRITE;

class PakFileCreator {
    static void createPakFile(@Nonnull Path path, @Nonnull String... entries) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.CREATE, WRITE)) {
            channel.position(12);

            List<PakFileEntry> pakFileEntries = new LinkedList<>();

            for (String entry : entries) {
                final int position = (int) channel.position();
                writeString(channel, entry);
                final int size = (int) channel.position() - position;

                PakFileEntry pakFileEntry = new PakFileEntry(entry, position, size);
                pakFileEntries.add(pakFileEntry);
            }

            final int fileTableOffset = (int) channel.position();

            for (PakFileEntry pakFileEntry : pakFileEntries) {
                writeString(channel, pakFileEntry.getName(), 56);
                writeInt(channel, pakFileEntry.getOffset());
                writeInt(channel, pakFileEntry.getSize());
            }

            final int fileTableLength = (int) channel.position() - fileTableOffset;

            channel.position(0);
            writeString(channel, "PACK");
            writeInt(channel, fileTableOffset);
            writeInt(channel, fileTableLength);
        }
    }

    private static void writeString(SeekableByteChannel channel, String s) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
        channel.write(buffer);
    }

    private static void writeString(SeekableByteChannel channel, String s, @SuppressWarnings("SameParameterValue") int size) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put(s.getBytes(StandardCharsets.UTF_8));
        buffer.position(0);
        channel.write(buffer);
    }

    private static void writeInt(SeekableByteChannel channel, int number) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(number);
        buffer.position(0);
        channel.write(buffer);
    }
}
