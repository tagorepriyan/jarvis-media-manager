package com.jarvis.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores metadata for a completed or in-progress media import.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "media_imports")
public class MediaImportRecord {

    @Id
    @Column(name = "import_id", nullable = false, length = 36)
    private String importId;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "source_content_type", length = 128)
    private String sourceContentType;

    @Column(name = "source_size_bytes", nullable = false)
    private long sourceSizeBytes;

    @Column(name = "checksum", nullable = false, length = 128)
    private String checksum;

    @Column(name = "checksum_algorithm", nullable = false, length = 32)
    private String checksumAlgorithm;

    @Column(name = "provider_type", nullable = false, length = 80)
    private String providerType;

    @Column(name = "chunk_size_bytes", nullable = false)
    private long chunkSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private MediaImportStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "failure_reason", length = 2_000)
    private String failureReason;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "media_import_chunks", joinColumns = @JoinColumn(name = "import_id"))
    @OrderBy("chunkNumber ASC")
    private List<MediaImportChunkRecord> uploadedParts = new ArrayList<>();

    public void addUploadedPart(MediaImportChunkRecord chunkRecord) {
        if (chunkRecord != null) {
            uploadedParts.add(chunkRecord);
        }
    }
}