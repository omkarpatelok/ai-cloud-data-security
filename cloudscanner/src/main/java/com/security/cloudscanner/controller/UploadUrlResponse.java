package com.security.cloudscanner.controller;

/**
 * Response for GET /api/upload-url — pre-signed PUT URL and the S3 object key to use with POST /api/scans.
 */
public record UploadUrlResponse(String uploadUrl, String fileKey) {
}
