package com.jarvis.dto;

import lombok.Builder;

/**
 * Response returned after a movie import completes successfully.
 */
@Builder
public record ImportMovieResponse(
        String importId,
        String originalFileName,
        String checksum,
        long chunkSizeBytes,
        int uploadedParts,
        String providerType,
        String status) {
}