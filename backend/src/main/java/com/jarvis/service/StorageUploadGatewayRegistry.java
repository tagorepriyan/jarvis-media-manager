package com.jarvis.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves the storage gateway for the configured provider type.
 */
@Service
public class StorageUploadGatewayRegistry {

    private final Map<String, StorageUploadGateway> gatewaysByProviderType;

    public StorageUploadGatewayRegistry(List<StorageUploadGateway> gateways) {
        this.gatewaysByProviderType = gateways.stream()
                .collect(Collectors.toMap(gateway -> gateway.providerType().toLowerCase(), gateway -> gateway));
    }

    public StorageUploadGateway resolve(String providerType) {
        StorageUploadGateway gateway = gatewaysByProviderType.get(providerType.toLowerCase());
        if (gateway == null) {
            throw new IllegalStateException("No storage upload gateway configured for provider type: " + providerType);
        }
        return gateway;
    }
}