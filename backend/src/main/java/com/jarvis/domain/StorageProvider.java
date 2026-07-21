package com.jarvis.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Abstracts a storage backend that can host file copies, regardless of vendor or transport.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StorageProvider {

    /** Stable business identifier for the storage provider. */
    @EqualsAndHashCode.Include
    @NotBlank
    @Size(max = 36)
    private String storageProviderId;

    /** Human-readable provider name. */
    @NotBlank
    @Size(max = 120)
    private String name;

    /** Provider category, such as NAS, cloud, external drive, or other backend type. */
    @NotBlank
    @Size(max = 80)
    private String providerType;

    /** Indicates whether the provider is currently available for use. */
    private boolean enabled;
}