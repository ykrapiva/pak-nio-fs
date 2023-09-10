package io.github.ykrapiva.pakfs;

import java.io.IOException;

class FileFormatException extends IOException {
    FileFormatException(String message) {
        super(message);
    }
}
