package com.jarvis.service;

import java.nio.file.Path;

/**
 * One sequential chunk generated from the source movie file.
 */
public record ChunkFile(int chunkNumber, Path path, long sizeBytes) {
}