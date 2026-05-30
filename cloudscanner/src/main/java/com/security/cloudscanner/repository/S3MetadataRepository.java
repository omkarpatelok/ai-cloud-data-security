package com.security.cloudscanner.repository;

import com.security.cloudscanner.entity.S3MetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface S3MetadataRepository extends JpaRepository<S3MetadataEntity, Long> {
}
