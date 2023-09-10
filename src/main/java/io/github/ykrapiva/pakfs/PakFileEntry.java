package io.github.ykrapiva.pakfs;

import lombok.Value;

@Value
class PakFileEntry {
    String name;
    int offset;
    int size;
}
