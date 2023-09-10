package io.github.ykrapiva.pakfs;

import lombok.EqualsAndHashCode;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.stream.Stream;

@EqualsAndHashCode(of = {
        "fileSystem",
        "entryPath"
})
class PakPath implements Path {
    public static final String ROOT = "/";

    final PakFileSystem fileSystem;
    final String entryPath;

    PakPath(@Nonnull PakFileSystem fileSystem) {
        this(fileSystem, ROOT);
    }

    PakPath(@Nonnull PakFileSystem fileSystem, @Nonnull String entryPath) {
        this.fileSystem = fileSystem;
        this.entryPath = entryPath;
    }

    @Override
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public Path getRoot() {
        if (ROOT.equals(entryPath)) {
            return null;
        }
        return new PakPath(fileSystem);
    }

    @Override
    public Path getFileName() {
        return Paths.get(entryPath);
    }

    @Override
    public Path getParent() {
        return getRoot();
    }

    @Override
    public int getNameCount() {
        return 1;
    }

    @Override
    public Path getName(int index) {
        if (index != 0) {
            throw new IllegalArgumentException("Index must be 0");
        }
        return getFileName();
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        throw new IllegalArgumentException("Sub paths not supported");
    }

    @Override
    public boolean startsWith(@Nonnull Path other) {
        return startsWith(other.toString());
    }

    @Override
    public boolean startsWith(@Nonnull String other) {
        return entryPath.startsWith(other);
    }

    @Override
    public boolean endsWith(@Nonnull Path other) {
        return endsWith(other.toString());
    }

    @Override
    public boolean endsWith(@Nonnull String other) {
        return entryPath.endsWith(other);
    }

    @Override
    public Path normalize() {
        return this;
    }

    @Override
    public Path resolve(Path other) {
        return resolve(other.toString());
    }

    @Override
    public Path resolve(@Nonnull String other) {
        String newEntryPath = ROOT.equals(entryPath) ? other : entryPath + "/" + other;
        return new PakPath(fileSystem, newEntryPath);
    }

    @Override
    public Path resolveSibling(@Nonnull Path other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path resolveSibling(@Nonnull String other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path relativize(@Nonnull Path other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI toUri() {
        return URI.create(fileSystem.provider.getScheme() + ":" + fileSystem.uri.getSchemeSpecificPart() + (ROOT.equals(entryPath) ? "" : "!" + entryPath));
    }

    @Override
    public Path toAbsolutePath() {
        return this;
    }

    @Override
    public Path toRealPath(@Nonnull LinkOption... options) {
        return this;
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(@Nonnull WatchService watcher, @Nonnull Kind<?>[] events, Modifier... modifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(@Nonnull WatchService watcher, @Nonnull Kind<?>... events) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Path> iterator() {
        return Stream.of(this).map(path -> (Path) path).iterator();
    }

    @Override
    public int compareTo(@Nonnull Path other) {
        return toString().compareTo(other.toString());
    }

    @Override
    public String toString() {
        return entryPath;
    }

    boolean isRoot() {
        return ROOT.equals(entryPath);
    }

    long size() {
        return fileSystem.size(this);
    }

    boolean exists() {
        return fileSystem.exists(this);
    }

    @Nonnull
    SeekableByteChannel newReadOnlyChannel() throws IOException {
        return fileSystem.newReadOnlyChannel(this);
    }

    @Nonnull
    static PakPath get(@Nonnull Path path) {
        return (PakPath) Paths.get(URI.create("pak:" + path));
    }
}
