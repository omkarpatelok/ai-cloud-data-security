package com.security.cloudscanner.scanner;

public class S3MetadataResult {

    private boolean publicBucket;
    private boolean encryptionEnabled;
    private boolean versioningEnabled;
    private long objectSize;
    private String lastModified;

    public boolean isPublicBucket() {
        return publicBucket;
    }

    public void setPublicBucket(boolean publicBucket) {
        this.publicBucket = publicBucket;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    public boolean isVersioningEnabled() {
        return versioningEnabled;
    }

    public void setVersioningEnabled(boolean versioningEnabled) {
        this.versioningEnabled = versioningEnabled;
    }

    public long getObjectSize() {
        return objectSize;
    }

    public void setObjectSize(long objectSize) {
        this.objectSize = objectSize;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
}
