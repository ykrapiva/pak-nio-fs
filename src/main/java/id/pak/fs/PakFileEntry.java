package id.pak.fs;

import lombok.Value;

@Value
class PakFileEntry {
    String name;
    int offset;
    int size;
}
