# Description

This is a [PAK](https://quakewiki.org/wiki/.pak) file format **read-only** Java NIO file system extension
implementation.

# Maven artifact

```
<dependency>
    <groupId>io.github.ykrapiva.pak-fs</groupId>
    <artifactId>pak-fs</artifactId>
    <version>1.0</version> <-- Check out the latest version -->
</dependency>
```

# Usage

```
Path rootPath = Paths.get(URI.create("pak:/path/to/file.pak"));

try (Stream<Path> entryStream = Files.list(rootPath)) {
    List<Path> paths = entryStream.collect(Collectors.toList());
    for (Path path : paths) {
        byte[] bytes = Files.readAllBytes(path);
        System.out.println(path + " -> " + bytes.length + " bytes");
    }
}
```

Output of this example:

```
palette.pcx -> 11 bytes
textures/texture.tga -> 20 bytes
maps/level1.bsp -> 15 bytes
maps/level2.bsp -> 15 bytes
```
