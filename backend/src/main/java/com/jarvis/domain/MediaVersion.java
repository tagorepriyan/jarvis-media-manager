package com.jarvis.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
 * A specific version of a media item, such as a theatrical cut, remaster, or localized edition.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MediaVersion {

    /** Stable business identifier for the media version. */
    @EqualsAndHashCode.Include
    @NotBlank
    @Size(max = 36)
    private String mediaVersionId;

    /** Human-readable label that distinguishes this version from others. */
    @NotBlank
    @Size(max = 120)
    private String versionLabel;

    /** Optional language tag associated with the version. */
    @Size(max = 32)
    private String languageTag;

    /** Indicates whether this is the primary version for the media item. */
    private boolean primaryVersion;

    /** Files that together represent this version. */
    @Valid
    @Builder.Default
    private List<File> files = new ArrayList<>();

    public void addFile(File file) {
        if (file != null) {
            files.add(file);
        }
    }

    public void removeFile(File file) {
        files.remove(file);
    }
}