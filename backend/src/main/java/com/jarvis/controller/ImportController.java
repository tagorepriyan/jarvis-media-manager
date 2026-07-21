package com.jarvis.controller;

import com.jarvis.dto.ImportMovieResponse;
import com.jarvis.service.ImportPipelineService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Entry point for the first media import workflow.
 */
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportPipelineService importPipelineService;

    public ImportController(ImportPipelineService importPipelineService) {
        this.importPipelineService = importPipelineService;
    }

    @PostMapping(value = "/movies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportMovieResponse importMovie(@RequestParam("file") MultipartFile file) {
        return importPipelineService.importMovie(file);
    }
}