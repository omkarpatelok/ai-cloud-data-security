package com.security.cloudscanner.controller;

import com.security.cloudscanner.entity.S3MetadataEntity;
import com.security.cloudscanner.repository.S3MetadataRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final S3MetadataRepository metadataRepository;

    public MetadataController(S3MetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    // 🔒 ADMIN-only (secured via Spring Security)
    @GetMapping
    public List<S3MetadataEntity> getAllMetadata() {
        return metadataRepository.findAll();
    }
}
