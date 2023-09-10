package id.pak.fs;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PakFileSystemProvider extends FileSystemProvider {
    private final Map<URI, PakFileSystem> fileSystems = new HashMap<>();

    @Override
    public String getScheme() {
        return "pak";
    }

    @Override
    public PakFileSystem newFileSystem(URI uri, Map<String, ?> env) {
        synchronized (fileSystems) {
            URI pakFileUri = createPakFileUri(uri);
            if (fileSystems.containsKey(pakFileUri)) {
                throw new FileSystemAlreadyExistsException(pakFileUri.toString());
            }
            PakFileSystem fs = new PakFileSystem(this, pakFileUri);
            fileSystems.put(pakFileUri, fs);
            return fs;
        }
    }

    @Override
    public PakFileSystem getFileSystem(URI uri) {
        synchronized (fileSystems) {
            URI pakFileUri = createPakFileUri(uri);
            PakFileSystem fs = fileSystems.get(pakFileUri);
            if (fs == null) {
                throw new FileSystemNotFoundException();
            }
            return fs;
        }
    }

    @Nonnull
    PakFileSystem getOrCreateFileSystem(@Nonnull URI uri) throws IOException {
        synchronized (fileSystems) {
            URI pakFileUri = createPakFileUri(uri);
            PakFileSystem fs = fileSystems.get(pakFileUri);
            if (fs == null) {
                fs = newFileSystem(pakFileUri, Collections.emptyMap());
            }
            return fs;
        }
    }

    @Override
    public PakPath getPath(@Nonnull URI uri) {
        final String entryPathSeparator = "!";
        String entryPath = PakPath.ROOT;

        String schemeSpecificPart = uri.getSchemeSpecificPart();
        int entryPathIndex = schemeSpecificPart.indexOf(entryPathSeparator);
        if (entryPathIndex != -1) {
            entryPath = schemeSpecificPart.substring(entryPathIndex + entryPathSeparator.length());
        }

        try {
            PakFileSystem fileSystem = getOrCreateFileSystem(uri);
            return fileSystem.getPath(entryPath);
        } catch (IOException e) {
            throw new FileSystemNotFoundException(e.getMessage());
        }
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        PakPath pakPath = toPakPath(path);
        if (options.isEmpty() || (options.size() == 1 && options.contains(StandardOpenOption.READ))) {
            return pakPath.newReadOnlyChannel();
        } else {
            throw new UnsupportedOperationException("Only " + StandardOpenOption.READ + " option is supported");
        }
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path path, Filter<? super Path> filter) throws IOException {
        PakPath pakPath = toPakPath(path);
        if (pakPath.isRoot()) {
            return new DirectoryStream<Path>() {
                @Override
                public Iterator<Path> iterator() {
                    return ((PakPath) path).fileSystem.iterator(filter);
                }

                @Override
                public void close() {

                }
            };
        } else {
            throw new NotDirectoryException(pakPath.toString());
        }
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Path path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSameFile(Path path, Path path2) {
        return path.toUri().equals(path2.toUri());
    }

    @Override
    public boolean isHidden(Path path) {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        PakPath pakPath = toPakPath(path);
        Path pakFilePath = pakPath.fileSystem.getPakFilePath();
        return pakFilePath.getFileSystem().provider().getFileStore(pakFilePath);
    }

    @Override
    public void checkAccess(Path path, @Nonnull AccessMode... modesArray) throws IOException {
        PakPath pakPath = toPakPath(path);
        Set<AccessMode> modes = new HashSet<>(Arrays.asList(modesArray));

        if (!modes.isEmpty()) {
            if (modes.size() > 1 || !modes.contains(AccessMode.READ)) {
                throw new AccessDeniedException(pakPath.toString());
            }
        }
        if (!pakPath.exists()) {
            throw new NoSuchFileException(pakPath.toString());
        }
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        PakPath pakPath = toPakPath(path);
        checkAccess(pakPath);
        return (A) new PakPathAttributes(pakPath);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) {
        return Collections.emptyMap();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    private URI createPakFileUri(@Nonnull URI uri) {
        String schemeSpecificPart = uri.getSchemeSpecificPart();
        int i = schemeSpecificPart.indexOf("!");
        if (i >= 0) {
            schemeSpecificPart = schemeSpecificPart.substring(0, i);
        }
        schemeSpecificPart = schemeSpecificPart.replaceAll(" ", "%20");
        return URI.create("file:" + schemeSpecificPart);
    }

    private PakPath toPakPath(@Nonnull Path path) {
        if (path instanceof PakPath) {
            return (PakPath) path;
        } else {
            throw new IllegalArgumentException("Path is not an instance of " + PakPath.class);
        }
    }
}
