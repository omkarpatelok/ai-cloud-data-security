package com.security.cloudscanner.scanner;

import com.security.cloudscanner.exception.S3AccessException;
import com.security.cloudscanner.exception.S3ObjectNotFoundException;
import com.security.cloudscanner.exception.ScanProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3ResourceScanner implements ResourceScanner {

    private final S3Client s3Client;
    private final LocalMockStorage localMockStorage;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.max-file-size}")
    private long maxFileSize;

    public S3ResourceScanner(S3Client s3Client, LocalMockStorage localMockStorage) {
        this.s3Client = s3Client;
        this.localMockStorage = localMockStorage;
    }

    @Override
    public String scan(String resourceName) {
        try {
            byte[] contentBytes = scanBytes(resourceName);
            return new String(contentBytes, StandardCharsets.UTF_8);
        } catch (ScanProcessingException e) {
            if (e.getMessage() != null && e.getMessage().contains("exceeds the configured max file size")) {
                return "[SKIPPED] File too large for deep scan";
            }
            throw e;
        }
    }

    public byte[] scanBytes(String resourceName) {
        if (localMockStorage.contains(resourceName)) {
            return localMockStorage.get(resourceName);
        }

        try {
            HeadObjectResponse head = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(resourceName)
                            .build()
            );

            if (head.contentLength() > maxFileSize) {
                throw new ScanProcessingException(
                        "S3 object '" + resourceName + "' exceeds the configured max file size of " + maxFileSize + " bytes.",
                        null
                );
            }

            try (InputStream is = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(resourceName)
                            .build()
            )) {
                return is.readAllBytes();
            }
        } catch (S3Exception e) {
            throw mapS3Exception(resourceName, "reading object bytes", e);
        } catch (SdkClientException e) {
            throw new S3AccessException(
                    "Unable to connect to AWS S3 while reading '" + resourceName
                            + "'. Check aws.region, AWS credentials, and network access.",
                    e
            );
        } catch (ScanProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new ScanProcessingException(
                    "Unexpected error while reading raw bytes for S3 object '" + resourceName + "'.",
                    e
            );
        }
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public S3MetadataResult scanMetadata(String resourceName) {
        if (localMockStorage.contains(resourceName)) {
            S3MetadataResult metadata = new S3MetadataResult();
            byte[] data = localMockStorage.get(resourceName);
            metadata.setObjectSize((long) data.length);
            metadata.setEncryptionEnabled(false);
            metadata.setLastModified(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            metadata.setVersioningEnabled(false);
            metadata.setPublicBucket(false);
            return metadata;
        }

        try {
            S3MetadataResult metadata = new S3MetadataResult();

            HeadObjectResponse head = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(resourceName)
                            .build()
            );

            metadata.setObjectSize(head.contentLength());
            metadata.setEncryptionEnabled(head.serverSideEncryption() != null);
            metadata.setLastModified(
                    head.lastModified()
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );

            GetBucketVersioningResponse versioning = s3Client.getBucketVersioning(
                    GetBucketVersioningRequest.builder()
                            .bucket(bucketName)
                            .build()
            );

            metadata.setVersioningEnabled("Enabled".equalsIgnoreCase(versioning.statusAsString()));

            try {
                s3Client.getBucketPolicy(
                        GetBucketPolicyRequest.builder()
                                .bucket(bucketName)
                                .build()
                );
                metadata.setPublicBucket(true);
            } catch (S3Exception e) {
                if (e.statusCode() == 404) {
                    metadata.setPublicBucket(false);
                } else {
                    throw e;
                }
            }

            return metadata;
        } catch (S3Exception e) {
            throw mapS3Exception(resourceName, "reading object metadata", e);
        } catch (SdkClientException e) {
            throw new S3AccessException(
                    "Unable to connect to AWS S3 while reading metadata for '" + resourceName
                            + "'. Check aws.region, AWS credentials, and network access.",
                    e
            );
        } catch (Exception e) {
            throw new ScanProcessingException(
                    "Unexpected error while reading metadata for S3 object '" + resourceName + "'.",
                    e
            );
        }
    }

    public void moveObjectToSecureBucket(String destinationBucket, String objectKey, String maskedContent) {
        if (localMockStorage.contains(objectKey)) {
            System.out.println("[SIMULATION] Moving masked local mock object to sensitive bucket: " + destinationBucket);
            byte[] data = maskedContent != null ? maskedContent.getBytes(java.nio.charset.StandardCharsets.UTF_8) : localMockStorage.get(objectKey);
            localMockStorage.put("sensitive-quarantine/" + objectKey, data);
            // Optionally remove from original mock path, but keep for simulation continuity if needed.
            return;
        }

        try {
            System.out.println("Moving " + objectKey + " to " + destinationBucket + " via AWS S3...");
            try {
                s3Client.headBucket(software.amazon.awssdk.services.s3.model.HeadBucketRequest.builder().bucket(destinationBucket).build());
            } catch (Exception e) {
                System.out.println("Sensitive bucket doesn't exist. Creating bucket: " + destinationBucket);
                s3Client.createBucket(software.amazon.awssdk.services.s3.model.CreateBucketRequest.builder().bucket(destinationBucket).build());
            }

            if (maskedContent != null) {
                s3Client.putObject(
                        software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                                .bucket(destinationBucket)
                                .key(objectKey)
                                .build(),
                        software.amazon.awssdk.core.sync.RequestBody.fromString(maskedContent)
                );
            } else {
                s3Client.copyObject(
                        CopyObjectRequest.builder()
                                .sourceBucket(bucketName)
                                .sourceKey(objectKey)
                                .destinationBucket(destinationBucket)
                                .destinationKey(objectKey)
                                .build()
                );
            }

            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build()
            );
            System.out.println("Successfully securely transferred configured masked object to sensitive bucket!");
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to transfer to sensitive bucket (" + destinationBucket + ")!");
            System.err.println("Make sure the destination bucket exactly exists in your AWS account and IAM has s3:CopyObject permissions: " + e.getMessage());
            throw new RuntimeException("Move failed", e);
        }
    }

    public List<String> listAllObjects() {
        List<String> keys = new ArrayList<>();

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Response response;
        do {
            response = s3Client.listObjectsV2(request);
            response.contents().forEach(obj -> keys.add(obj.key()));
            request = request.toBuilder()
                    .continuationToken(response.nextContinuationToken())
                    .build();
        } while (response.isTruncated());

        return keys;
    }

    private RuntimeException mapS3Exception(String resourceName, String action, S3Exception e) {
        String awsMessage = e.awsErrorDetails() != null
                ? e.awsErrorDetails().errorMessage()
                : e.getMessage();

        if (e.statusCode() == 404) {
            return new S3ObjectNotFoundException(
                    "S3 object '" + resourceName + "' was not found while " + action
                            + " in bucket '" + bucketName + "'. Check the object key and aws.s3.bucket configuration.",
                    e
            );
        }

        if (e.statusCode() == 403) {
            return new S3AccessException(
                    "Access denied while " + action + " for S3 object '" + resourceName
                            + "' in bucket '" + bucketName + "'. Check IAM permissions for GetObject, HeadObject, and bucket access.",
                    e
            );
        }

        return new S3AccessException(
                "AWS S3 returned status " + e.statusCode() + " while " + action
                        + " for object '" + resourceName + "' in bucket '" + bucketName + "'. Message: " + awsMessage,
                e
        );
    }
}
