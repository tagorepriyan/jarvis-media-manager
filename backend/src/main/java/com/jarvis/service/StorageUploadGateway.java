package com.jarvis.service;

import com.jarvis.entity.MediaImportRecord;

/**
 * Port for uploading a chunk to a storage backend.
 */
public interface StorageUploadGateway {

    /** Returns the provider type that this gateway supports. */
    String providerType();

    /** Uploads one chunk sequentially and returns the persisted storage reference. */
    UploadedChunk upload(MediaImportRecord importRecord, ChunkFile chunkFile);
}