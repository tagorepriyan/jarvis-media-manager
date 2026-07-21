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
 * Catalog entry representing a film in the JARVIS media library.
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Movie extends MediaItem {

    /** Original title as released or marketed. */
    @Size(max = 255)
    private String originalTitle;

    /** Year the movie was first released. */
    @Min(1888)
    private Integer releaseYear;

    /** Approximate runtime in minutes. */
    @PositiveOrZero
    private Integer runtimeMinutes;
}