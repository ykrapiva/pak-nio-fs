package id.pak.fs;

import java.io.IOException;

class FileFormatException extends IOException {
    FileFormatException(String message) {
        super(message);
    }
}
