package com.jarvis.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Base abstraction for any cataloged media item managed by JARVIS.
 *
 * <p>A media item groups one or more logical versions of the same title,
 * independent of where the underlying files are stored.</p>
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class MediaItem {

    /** Stable business identifier for the media item. */
    @EqualsAndHashCode.Include
    @NotBlank
    @Size(max = 36)
    private String mediaItemId;

    /** Human-readable title used in the catalog. */
    @NotBlank
    @Size(max = 255)
    private String title;

    /** Optional descriptive text for search and discovery. */
    @Size(max = 2_000)
    private String description;

    /**
     * Logical versions that belong to this media item.
     * A media item can have multiple versions for different cuts, languages, or encodes.
     */
    @Valid
    @lombok.Builder.Default
    private List<MediaVersion> versions = new ArrayList<>();

    public void addVersion(MediaVersion version) {
        if (version != null) {
            versions.add(version);
        }
    }

    public void removeVersion(MediaVersion version) {
        versions.remove(version);
    }
}