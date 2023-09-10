package id.pak.fs;

import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;

class PakFileEntryChannel implements SeekableByteChannel {
    private static final int BUF_SIZE = 8192;

    private final FileChannel fileChannel;
    private final PakFileEntry pakFileEntry;
    private final long maxPosition;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);

    @SneakyThrows
    public PakFileEntryChannel(FileChannel fileChannel, PakFileEntry pakFileEntry) {
        this.fileChannel = fileChannel;
        this.pakFileEntry = pakFileEntry;
        this.fileChannel.position(pakFileEntry.getOffset());
        this.maxPosition = fileChannel.position() + pakFileEntry.getSize();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int bytesLeft = (int) (maxPosition - fileChannel.position());
        if (bytesLeft <= 0) {
            return -1; // EOF
        }

        buffer.position(0);
        buffer.limit(Math.min(bytesLeft, buffer.capacity()));

        int read = fileChannel.read(buffer);

        dst.put(buffer.array(), buffer.arrayOffset(), read);

        return read;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if (newPosition < 0 || newPosition > maxPosition) {
            throw new IllegalArgumentException();
        }
        return fileChannel.position(pakFileEntry.getOffset() + newPosition);
    }

    @Override
    public long size() {
        return pakFileEntry.getSize();
    }

    @Override
    public int write(ByteBuffer src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long position() throws IOException {
        return fileChannel.position() - pakFileEntry.getOffset();
    }

    @Override
    public SeekableByteChannel truncate(long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        return fileChannel.isOpen();
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}
