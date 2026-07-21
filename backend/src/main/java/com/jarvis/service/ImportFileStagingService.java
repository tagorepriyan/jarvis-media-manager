package com.jarvis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stages the uploaded source movie on disk so it can be processed without loading it into memory.
 */
@Slf4j
@Service
public class ImportFileStagingService {

    public StagedImportFile stage(MultipartFile file) {
        try {
            Path stagingDirectory = Files.createTempDirectory("jarvis-import-");
            String originalFileName = file.getOriginalFilename() == null ? "unknown.mkv" : file.getOriginalFilename();
            String safeName = UUID.randomUUID() + "-" + originalFileName;
            Path stagedFile = stagingDirectory.resolve(safeName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, stagedFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return new StagedImportFile(stagedFile, originalFileName, file.getContentType(), file.getSize(), stagingDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to stage uploaded movie file", exception);
        }
    }

    /**
     * Staged source file metadata.
     */
    public record StagedImportFile(Path stagedFile, String originalFileName, String contentType, long sizeBytes,
                                   Path stagingDirectory) {
    }
}