package id.pak.fs;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PakPathTest {
    private final PakPath subject = PakPath.get(Paths.get("/tmp/test.pak"));
    private final PakPath entrySubject = PakPath.get(Paths.get("/tmp/test.pak!maps/level.bsp"));

    @Test
    void getFileSystem() {
        assertThat(subject.getFileSystem()).isInstanceOf(PakFileSystem.class);
    }

    @Test
    void isAbsolute() {
        assertThat(subject.isAbsolute()).isTrue();
        assertThat(entrySubject.isAbsolute()).isTrue();
    }

    @Test
    void getRoot() {
        assertThat(subject.getRoot()).isNull();
        assertThat(entrySubject.getRoot()).isEqualTo(subject);
    }

    @Test
    void getFileName() {
        assertThat(subject.getFileName().toString()).isEqualTo("/");
        assertThat(entrySubject.getFileName().toString()).isEqualTo("maps/level.bsp");
    }

    @Test
    void getParent() {
        assertThat(subject.getParent()).isNull();
        assertThat(entrySubject.getParent()).isEqualTo(subject);
    }

    @Test
    void getNameCount() {
        assertThat(subject.getNameCount()).isEqualTo(1);
        assertThat(entrySubject.getNameCount()).isEqualTo(1);
    }

    @Test
    void getName() {
        assertThat(subject.getName(0).toString()).isEqualTo("/");
        assertThat(entrySubject.getName(0).toString()).isEqualTo("maps/level.bsp");
        assertThrows(IllegalArgumentException.class, () -> subject.getName(1));
        assertThrows(IllegalArgumentException.class, () -> entrySubject.getName(1));
    }

    @Test
    void subpath() {
        assertThrows(IllegalArgumentException.class, () -> subject.subpath(0, 1));
        assertThrows(IllegalArgumentException.class, () -> entrySubject.subpath(0, 1));
    }

    @Test
    @SuppressWarnings("DuplicateExpressions")
    void startsWith() {
        assertThat(subject.startsWith(Paths.get("/"))).isTrue();
        assertThat(subject.startsWith(Paths.get("./"))).isFalse();
        assertThat(entrySubject.startsWith(Paths.get("/"))).isFalse();
        assertThat(entrySubject.startsWith(Paths.get("maps"))).isTrue();
    }

    @Test
    void endsWith() {
        assertThat(subject.endsWith(Paths.get("/"))).isTrue();
        assertThat(subject.endsWith(Paths.get("/./"))).isFalse();
        assertThat(entrySubject.endsWith(Paths.get("."))).isFalse();
        assertThat(entrySubject.endsWith(Paths.get(".bsp"))).isTrue();
    }

    @Test
    void normalize() {
        assertThat(subject.normalize()).isSameAs(subject);
        assertThat(entrySubject.normalize()).isSameAs(entrySubject);
    }

    @Test
    void resolve() {
        assertThat(subject.resolve("maps/level.bsp")).isEqualTo(entrySubject);
        assertThat(subject.resolve("maps").resolve("level.bsp")).isEqualTo(entrySubject);
        assertThat(subject.resolve(entrySubject)).isEqualTo(entrySubject);
    }

    @Test
    void resolveSibling() {
        assertThrows(UnsupportedOperationException.class, () -> subject.resolveSibling(entrySubject));
        assertThrows(UnsupportedOperationException.class, () -> subject.resolveSibling("maps"));
    }

    @Test
    void relativize() {
        assertThrows(UnsupportedOperationException.class, () -> subject.relativize(entrySubject));
    }

    @Test
    void toUri() {
        assertThat(subject.toUri().toString()).isEqualTo("pak:/tmp/test.pak");
        assertThat(entrySubject.toUri().toString()).isEqualTo("pak:/tmp/test.pak!maps/level.bsp");
    }

    @Test
    void toAbsolutePath() {
        assertThat(subject.toAbsolutePath()).isSameAs(subject);
        assertThat(entrySubject.toAbsolutePath()).isSameAs(entrySubject);
    }

    @Test
    void toRealPath() {
        assertThat(subject.toRealPath()).isSameAs(subject);
        assertThat(entrySubject.toRealPath()).isSameAs(entrySubject);
    }

    @Test
    void toFile() {
        assertThrows(UnsupportedOperationException.class, subject::toFile);
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void register() {
        assertThrows(UnsupportedOperationException.class, () -> subject.register(null, null, (Modifier[]) null));
        assertThrows(UnsupportedOperationException.class, () -> subject.register(null, (Kind<?>) null));
    }

    @Test
    void iterator() {
        List<Path> subjectIteratorElements = new ArrayList<>();
        subject.iterator().forEachRemaining(subjectIteratorElements::add);
        assertThat(subjectIteratorElements).containsExactly(subject);

        List<Path> entrySubjectIteratorElements = new ArrayList<>();
        entrySubject.iterator().forEachRemaining(entrySubjectIteratorElements::add);
        assertThat(entrySubjectIteratorElements).containsExactly(entrySubject);
    }

    @Test
    void compareTo() {
        assertThat(subject).usingDefaultComparator().isLessThan(entrySubject);
        assertThat(subject).usingDefaultComparator().isEqualByComparingTo(subject);
    }

    @Test
    void toStringTest() {
        assertThat(subject.toString()).isEqualTo("/");
        assertThat(entrySubject.toString()).isEqualTo("maps/level.bsp");
    }

    @Test
    void isRoot() {
        assertThat(subject.isRoot()).isTrue();
        assertThat(entrySubject.isRoot()).isFalse();
    }
}