package com.jarvis.repository;

import com.jarvis.entity.MediaImportRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persists media import metadata.
 */
public interface MediaImportRecordRepository extends JpaRepository<MediaImportRecord, String> {
}