package com.jarvis.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A concrete copy of a file that exists on a specific storage provider.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StorageCopy {

    /** Stable business identifier for the storage copy. */
    @EqualsAndHashCode.Include
    @NotBlank
    @Size(max = 36)
    private String storageCopyId;

    /** Provider-specific key or path that locates the copy. */
    @NotBlank
    @Size(max = 255)
    private String storageKey;

    /** Provider that hosts this copy. */
    @NotNull
    @Valid
    private StorageProvider storageProvider;

    /** Current lifecycle state of the copy. */
    @NotBlank
    @Size(max = 40)
    private String copyStatus;

    /** Time when the copy was last verified for integrity. */
    private Instant verifiedAt;

    /** Time when the copy was last synchronized or refreshed. */
    private Instant lastSyncedAt;
}