package com.jarvis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Configuration for the media import pipeline.
 */
@ConfigurationProperties(prefix = "jarvis.import")
public class ImportProperties {

    private DataSize chunkSize = DataSize.ofMegabytes(1900);

    private String defaultProviderType = "telegram";

    public DataSize getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(DataSize chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getDefaultProviderType() {
        return defaultProviderType;
    }

    public void setDefaultProviderType(String defaultProviderType) {
        this.defaultProviderType = defaultProviderType;
    }
}