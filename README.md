# Description

This is a [PAK](https://quakewiki.org/wiki/.pak) file format **read-only** Java NIO file system extension
implementation.

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
