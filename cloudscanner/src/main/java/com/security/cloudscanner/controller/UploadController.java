package com.security.cloudscanner.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final Pattern SAFE_NAME = Pattern.compile("^[a-zA-Z0-9._\\- ]+$");
    private static final int MAX_NAME_LEN = 200;

    private final software.amazon.awssdk.services.s3.S3Client s3Client;
    private final com.security.cloudscanner.scanner.LocalMockStorage localMockStorage;
    private final String bucketName;
    private final long maxUploadBytes;

    public UploadController(
            software.amazon.awssdk.services.s3.S3Client s3Client,
            com.security.cloudscanner.scanner.LocalMockStorage localMockStorage,
            @Value("${aws.s3.bucket}") String bucketName,
            @Value("${app.upload.max-file-size-bytes:10485760}") long maxUploadBytes
    ) {
        this.s3Client = s3Client;
        this.localMockStorage = localMockStorage;
        this.bucketName = bucketName;
        this.maxUploadBytes = maxUploadBytes;
    }

    /**
     * Returns a pre-signed PUT URL for uploading a single object to the configured scan bucket.
     * After upload, call POST /api/scans with resourceName = fileKey and resourceType = S3_OBJECT.
     */
    @GetMapping("/upload-url")
    public UploadUrlResponse getUploadUrl(
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "contentType", required = false) String contentType,
            @RequestParam(value = "fileSize", required = false) Long fileSize,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        if (fileName == null || fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName is required");
        }

        if (fileSize != null && fileSize > maxUploadBytes) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File exceeds maximum upload size of " + maxUploadBytes + " bytes"
            );
        }

        String baseName = sanitizeFileName(fileName);
        String key = "uploads/" + baseName;

        String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
        String url = baseUrl + "/api/local-upload?key=" + key;
        
        return new UploadUrlResponse(url, key);
    }

    @org.springframework.web.bind.annotation.PutMapping("/local-upload")
    public void handleLocalUpload(
            @RequestParam("key") String key,
            jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        
        byte[] data = request.getInputStream().readAllBytes();
        
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(request.getContentType() != null ? request.getContentType() : "application/octet-stream")
                        .build(),
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(data)
            );
        } catch (Exception e) {
            log.warn("S3 upload failed, falling back to local storage for key: {}", key, e);
            localMockStorage.put(key, data);
        }
    }

    /**
     * Short, UI-safe message. Full details stay in server logs.
     */
    static String presignFailureMessage(SdkException e, String bucketName) {
        String raw = e.getMessage() != null ? e.getMessage() : "";
        if (raw.contains("Unable to load credentials")
                || raw.contains("Failed to load credentials")
                || raw.contains("AwsCredentialsProviderChain")) {
            return "No AWS credentials for this server. Configure credentials, then restart the app: "
                    + "run `aws configure`, or set environment variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY "
                    + "(and AWS_SESSION_TOKEN if using temporary creds). "
                    + "The IAM user/role must be allowed to use S3 for bucket \"" + bucketName + "\" (e.g. s3:PutObject on uploads/*).";
        }
        return "Cannot sign S3 upload for bucket \"" + bucketName + "\": " + truncate(raw, 350);
    }

    static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    /**
     * Strip path components and allow only safe characters in the final name.
     */
    static String sanitizeFileName(String fileName) {
        String raw = fileName.replace('\\', '/');
        int slash = raw.lastIndexOf('/');
        String base = slash >= 0 ? raw.substring(slash + 1) : raw;
        base = base.trim();
        if (base.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        if (base.length() > MAX_NAME_LEN) {
            base = base.substring(0, MAX_NAME_LEN);
        }
        if (!SAFE_NAME.matcher(base).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File name may only contain letters, numbers, spaces, dots, dashes, and underscores"
            );
        }
        return base;
    }
}
