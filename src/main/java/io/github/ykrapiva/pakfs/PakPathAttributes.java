package io.github.ykrapiva.pakfs;

import lombok.RequiredArgsConstructor;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

@RequiredArgsConstructor
class PakPathAttributes implements BasicFileAttributes {
    final PakPath path;

    @Override
    public FileTime lastModifiedTime() {
        return null;
    }

    @Override
    public FileTime lastAccessTime() {
        return null;
    }

    @Override
    public FileTime creationTime() {
        return null;
    }

    @Override
    public boolean isRegularFile() {
        return !path.isRoot();
    }

    @Override
    public boolean isDirectory() {
        return path.isRoot();
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        return path.size();
    }

    @Override
    public Object fileKey() {
        return null;
    }
}
