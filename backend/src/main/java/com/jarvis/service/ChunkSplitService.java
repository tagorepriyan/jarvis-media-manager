package com.jarvis.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits a staged media file into sequential fixed-size chunks.
 */
@Service
public class ChunkSplitService {

    public List<ChunkFile> split(Path sourceFile, long chunkSizeBytes) {
        if (chunkSizeBytes <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than zero");
        }

        try {
            Path chunkDirectory = Files.createTempDirectory(sourceFile.getParent(), "jarvis-chunks-");
            List<ChunkFile> chunks = new ArrayList<>();

            try (InputStream inputStream = Files.newInputStream(sourceFile)) {
                byte[] buffer = new byte[8192];
                int chunkNumber = 1;
                long chunkBytesWritten = 0L;
                OutputStream currentOutputStream = null;
                Path currentChunkPath = null;

                try {
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        int offset = 0;
                        while (offset < read) {
                            if (currentOutputStream == null) {
                                currentChunkPath = chunkDirectory.resolve(buildChunkFileName(chunkNumber));
                                currentOutputStream = Files.newOutputStream(currentChunkPath,
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.TRUNCATE_EXISTING,
                                        StandardOpenOption.WRITE);
                            }

                            long remainingInChunk = chunkSizeBytes - chunkBytesWritten;
                            int bytesToWrite = (int) Math.min(remainingInChunk, read - offset);
                            currentOutputStream.write(buffer, offset, bytesToWrite);
                            offset += bytesToWrite;
                            chunkBytesWritten += bytesToWrite;

                            if (chunkBytesWritten == chunkSizeBytes) {
                                currentOutputStream.close();
                                chunks.add(new ChunkFile(chunkNumber, currentChunkPath, chunkBytesWritten));
                                currentOutputStream = null;
                                currentChunkPath = null;
                                chunkNumber++;
                                chunkBytesWritten = 0L;
                            }
                        }
                    }

                    if (currentOutputStream != null) {
                        currentOutputStream.close();
                        chunks.add(new ChunkFile(chunkNumber, currentChunkPath, chunkBytesWritten));
                    }

                    return chunks;
                } finally {
                    if (currentOutputStream != null) {
                        try {
                            currentOutputStream.close();
                        } catch (IOException ignored) {
                            // Best-effort cleanup only.
                        }
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to split staged movie file", exception);
        }
    }

    private String buildChunkFileName(int chunkNumber) {
        return String.format("chunk-%05d.part", chunkNumber);
    }
}