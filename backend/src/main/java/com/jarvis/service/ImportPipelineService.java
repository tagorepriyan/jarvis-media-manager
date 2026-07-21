package com.jarvis.service;

import com.jarvis.config.ImportProperties;
import com.jarvis.dto.ImportMovieResponse;
import com.jarvis.entity.MediaImportChunkRecord;
import com.jarvis.entity.MediaImportRecord;
import com.jarvis.entity.MediaImportStatus;
import com.jarvis.repository.MediaImportRecordRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Orchestrates the end-to-end movie import flow.
 */
@Service
public class ImportPipelineService {

    private static final String SHA_256 = "SHA-256";

    private final ImportProperties importProperties;
    private final ImportFileStagingService fileStagingService;
    private final Sha256ChecksumService checksumService;
    private final ChunkSplitService chunkSplitService;
    private final StorageUploadGatewayRegistry storageUploadGatewayRegistry;
    private final MediaImportRecordRepository mediaImportRecordRepository;

    public ImportPipelineService(ImportProperties importProperties,
                                 ImportFileStagingService fileStagingService,
                                 Sha256ChecksumService checksumService,
                                 ChunkSplitService chunkSplitService,
                                 StorageUploadGatewayRegistry storageUploadGatewayRegistry,
                                 MediaImportRecordRepository mediaImportRecordRepository) {
        this.importProperties = importProperties;
        this.fileStagingService = fileStagingService;
        this.checksumService = checksumService;
        this.chunkSplitService = chunkSplitService;
        this.storageUploadGatewayRegistry = storageUploadGatewayRegistry;
        this.mediaImportRecordRepository = mediaImportRecordRepository;
    }

    @Transactional
    public ImportMovieResponse importMovie(MultipartFile file) {
        validateMovieFile(file);

        ImportFileStagingService.StagedImportFile stagedImportFile = fileStagingService.stage(file);
        Path stagedFile = stagedImportFile.stagedFile();
        String importId = UUID.randomUUID().toString();
        String providerType = importProperties.getDefaultProviderType();
        long chunkSizeBytes = importProperties.getChunkSize().toBytes();
        String checksum = checksumService.calculate(stagedFile);
        Path chunkDirectory = null;

        MediaImportRecord mediaImportRecord = MediaImportRecord.builder()
                .importId(importId)
                .originalFileName(stagedImportFile.originalFileName())
                .sourceContentType(stagedImportFile.contentType())
                .sourceSizeBytes(stagedImportFile.sizeBytes())
                .checksum(checksum)
                .checksumAlgorithm(SHA_256)
                .providerType(providerType)
                .chunkSizeBytes(chunkSizeBytes)
                .status(MediaImportStatus.CREATED)
                .startedAt(Instant.now())
                .build();

        mediaImportRecordRepository.save(mediaImportRecord);

        List<ChunkFile> chunks = chunkSplitService.split(stagedFile, chunkSizeBytes);
        if (!chunks.isEmpty()) {
            chunkDirectory = chunks.get(0).path().getParent();
        }
        StorageUploadGateway storageUploadGateway = storageUploadGatewayRegistry.resolve(providerType);

        mediaImportRecord.setStatus(MediaImportStatus.UPLOADING);
        mediaImportRecordRepository.save(mediaImportRecord);

        try {
            for (ChunkFile chunkFile : chunks) {
                UploadedChunk uploadedChunk = storageUploadGateway.upload(mediaImportRecord, chunkFile);
                mediaImportRecord.addUploadedPart(MediaImportChunkRecord.builder()
                        .chunkNumber(uploadedChunk.chunkNumber())
                        .storageKey(uploadedChunk.storageKey())
                        .sizeBytes(uploadedChunk.sizeBytes())
                        .uploadedAt(uploadedChunk.uploadedAt())
                        .build());
                mediaImportRecordRepository.save(mediaImportRecord);
            }

            mediaImportRecord.setStatus(MediaImportStatus.COMPLETED);
            mediaImportRecord.setFinishedAt(Instant.now());
            mediaImportRecordRepository.save(mediaImportRecord);

            return ImportMovieResponse.builder()
                    .importId(mediaImportRecord.getImportId())
                    .originalFileName(mediaImportRecord.getOriginalFileName())
                    .checksum(mediaImportRecord.getChecksum())
                    .chunkSizeBytes(mediaImportRecord.getChunkSizeBytes())
                    .uploadedParts(mediaImportRecord.getUploadedParts().size())
                    .providerType(mediaImportRecord.getProviderType())
                    .status(mediaImportRecord.getStatus().name())
                    .build();
        } catch (RuntimeException exception) {
            mediaImportRecord.setStatus(MediaImportStatus.FAILED);
            mediaImportRecord.setFinishedAt(Instant.now());
            mediaImportRecord.setFailureReason(exception.getMessage());
            mediaImportRecordRepository.save(mediaImportRecord);
            throw exception;
        } finally {
            cleanupTempDirectory(chunkDirectory);
            cleanupTempDirectory(stagedImportFile.stagingDirectory());
        }
    }

    private void validateMovieFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Movie file is required");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.toLowerCase(Locale.ROOT).endsWith(".mkv")) {
            throw new IllegalArgumentException("Only MKV movie files are supported for the import pipeline");
        }
    }

    private void cleanupTempDirectory(Path stagingDirectory) {
        if (stagingDirectory == null || !Files.exists(stagingDirectory)) {
            return;
        }

        try (var paths = Files.walk(stagingDirectory)) {
            paths.sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                            // Best-effort cleanup only.
                        }
                    });
        } catch (Exception ignored) {
            // Best-effort cleanup only.
        }
    }
}