package com.security.cloudscanner.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "s3_metadata")
public class S3MetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String resourceName;
    private boolean publicBucket;
    private boolean encryptionEnabled;
    private boolean versioningEnabled;
    private long objectSize;
    private String lastModified;

    private LocalDateTime scannedAt;

    // getters & setters
}
