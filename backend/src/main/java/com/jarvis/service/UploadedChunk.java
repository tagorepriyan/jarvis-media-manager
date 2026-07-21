package com.jarvis.service;

import java.time.Instant;

/**
 * Result returned after one chunk is uploaded to a storage provider.
 */
public record UploadedChunk(int chunkNumber, String storageKey, long sizeBytes, Instant uploadedAt) {
}