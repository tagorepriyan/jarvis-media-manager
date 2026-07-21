package com.jarvis.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A physical binary artifact that stores the actual media content managed by JARVIS.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class File {

    /** Stable business identifier for the file. */
    @EqualsAndHashCode.Include
    @NotBlank
    @Size(max = 36)
    private String fileId;

    /** Original or logical filename used to identify the asset. */
    @NotBlank
    @Size(max = 255)
    private String fileName;

    /** MIME type of the file. */
    @NotBlank
    @Size(max = 128)
    private String mimeType;

    /** Size of the file in bytes. */
    @PositiveOrZero
    private long sizeBytes;

    /** Checksum used for integrity verification. */
    @NotBlank
    @Size(max = 128)
    private String checksum;

    /** Hash algorithm used to generate the checksum. */
    @NotBlank
    @Size(max = 32)
    private String checksumAlgorithm;

    /** Copies of this file that exist on storage providers. */
    @Valid
    @Builder.Default
    private List<StorageCopy> storageCopies = new ArrayList<>();

    public void addStorageCopy(StorageCopy storageCopy) {
        if (storageCopy != null) {
            storageCopies.add(storageCopy);
        }
    }

    public void removeStorageCopy(StorageCopy storageCopy) {
        storageCopies.remove(storageCopy);
    }
}