package id.pak.fs;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@EqualsAndHashCode(of = "uri", callSuper = false)
@ToString
class PakFileSystem extends FileSystem {
    static final String SEPARATOR = "/";

    final PakFileSystemProvider provider;
    final URI uri;

    final PakFile pakFile;

    public PakFileSystem(@Nonnull PakFileSystemProvider provider, @Nonnull URI uri) {
        this.provider = provider;
        this.uri = uri;
        this.pakFile = new PakFile(Paths.get(uri));
    }

    @Override
    public FileSystemProvider provider() {
        return this.provider;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton(new PakPath(this));
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.emptyList();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.singleton("basic");
    }

    @Override
    public PakPath getPath(@Nonnull String first, @Nonnull String... more) {
        StringBuilder sb = new StringBuilder();
        sb.append(first);

        for (String path : more) {
            if (path.length() > 0) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(path);
            }
        }

        String entryPath = sb.toString();
        if (entryPath.isEmpty()) {
            entryPath = PakPath.ROOT;
        }

        return new PakPath(this, entryPath);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    Iterator<Path> iterator(@Nonnull DirectoryStream.Filter<? super Path> filter) {
        try {
            return pakFile.getEntries().values().stream()
                    .map(entry -> (Path) new PakPath(this, entry.getName()))
                    .filter(path -> isAccepted(filter, path))
                    .iterator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private boolean isAccepted(DirectoryStream.Filter<? super Path> filter, Path path) {
        return filter.accept(path);
    }

    @Nonnull
    Path getPakFilePath() {
        return pakFile.getPath();
    }

    long size(@Nonnull PakPath path) {
        try {
            return Optional.ofNullable(pakFile.getEntries().get(path.entryPath))
                    .map(PakFileEntry::getSize)
                    .orElse(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    boolean exists(@Nonnull PakPath path) {
        try {
            return path.isRoot() || pakFile.getEntries().containsKey(path.entryPath);
        } catch (IOException ex) {
            return false;
        }
    }

    @Nonnull
    SeekableByteChannel newReadOnlyChannel(@Nonnull PakPath path) throws IOException {
        PakFileEntry pakFileEntry = pakFile.getEntries().get(path.entryPath);
        if (pakFileEntry == null) {
            throw new NoSuchElementException(path.entryPath);
        }

        return pakFile.newReadOnlyChannel(pakFileEntry);
    }
}
