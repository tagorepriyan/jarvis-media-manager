package com.jarvis.telegram;

import com.jarvis.entity.MediaImportRecord;
import com.jarvis.service.ChunkFile;
import com.jarvis.service.StorageUploadGateway;
import com.jarvis.service.UploadedChunk;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

/**
 * Telegram-backed storage gateway used by the first import slice.
 *
 * <p>The implementation is isolated behind the upload port so NAS or another provider
 * can replace it later without changing the import workflow.</p>
 */
@Service
public class TelegramStorageUploadGateway implements StorageUploadGateway {

    @Override
    public String providerType() {
        return "telegram";
    }

    @Override
    public UploadedChunk upload(MediaImportRecord importRecord, ChunkFile chunkFile) {
        try {
            Path outboxDirectory = Path.of(System.getProperty("java.io.tmpdir"), "jarvis-telegram-outbox", importRecord.getImportId());
            Files.createDirectories(outboxDirectory);

            Path uploadedPath = outboxDirectory.resolve(String.format("part-%05d-%s", chunkFile.chunkNumber(), UUID.randomUUID()));
            Files.copy(chunkFile.path(), uploadedPath, StandardCopyOption.REPLACE_EXISTING);

            return new UploadedChunk(
                    chunkFile.chunkNumber(),
                    uploadedPath.toUri().toString(),
                    chunkFile.sizeBytes(),
                    Instant.now());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to upload chunk through the Telegram gateway", exception);
        }
    }
}