package com.jarvis.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Catalog entry representing an audio release or music item in the JARVIS media library.
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Music extends MediaItem {

    /** Primary artist or performer associated with the music item. */
    @Size(max = 255)
    private String primaryArtist;

    /** Album, release, or collection name for the music item. */
    @Size(max = 255)
    private String releaseTitle;

    /** Year the music item was released. */
    @Min(1888)
    private Integer releaseYear;

    /** Approximate runtime in minutes. */
    @PositiveOrZero
    private Integer runtimeMinutes;
}