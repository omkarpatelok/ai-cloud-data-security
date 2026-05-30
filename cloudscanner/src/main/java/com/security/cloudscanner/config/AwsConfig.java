package com.security.cloudscanner.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class AwsConfig {

    private static final Logger log = LoggerFactory.getLogger(AwsConfig.class);

    @Value("${aws.region}")
    private String awsRegion;

    /**
     * Resolves credentials in order:
     * <ol>
     *   <li>{@code AWS_ACCESS_KEY_ID} / {@code AWS_SECRET_ACCESS_KEY} from the OS environment</li>
     *   <li>Same keys from {@code .env} (parsed line-by-line; works with CRLF / UTF-8 BOM)</li>
     *   <li>Default AWS SDK chain ({@code ~/.aws/credentials}, IAM role, etc.)</li>
     * </ol>
     */
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        Path dotenvDir = resolveDotenvDirectory();
        Path envFile = dotenvDir.resolve(".env");
        Map<String, String> fileVars = parseEnvFile(envFile);

        if (Files.isRegularFile(envFile)) {
            log.info("Read .env from {}", envFile.toAbsolutePath());
        } else {
            log.info("No .env at {} — using environment variables or default AWS credential chain", envFile.toAbsolutePath());
        }

        String accessKey = firstNonBlank(
                System.getenv("AWS_ACCESS_KEY_ID"),
                stripQuotes(fileVars.get("AWS_ACCESS_KEY_ID"))
        );
        String secretKey = firstNonBlank(
                System.getenv("AWS_SECRET_ACCESS_KEY"),
                stripQuotes(fileVars.get("AWS_SECRET_ACCESS_KEY"))
        );
        String sessionToken = firstNonBlank(
                System.getenv("AWS_SESSION_TOKEN"),
                stripQuotes(fileVars.get("AWS_SESSION_TOKEN"))
        );

        if (accessKey != null && secretKey != null) {
            log.info("AWS API credentials: using static credentials from environment or .env");
            if (sessionToken != null && !sessionToken.isBlank()) {
                return StaticCredentialsProvider.create(
                        AwsSessionCredentials.create(accessKey, secretKey, sessionToken)
                );
            }
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            );
        }

        log.info("AWS API credentials: using DefaultCredentialsProvider (e.g. ~/.aws/credentials)");
        return DefaultCredentialsProvider.create();
    }

    /**
     * KEY=VALUE lines; ignores {@code #} comments; strips UTF-8 BOM; supports quoted values.
     */
    static Map<String, String> parseEnvFile(Path envFile) {
        Map<String, String> out = new LinkedHashMap<>();
        if (!Files.isRegularFile(envFile)) {
            return out;
        }
        try {
            String content = Files.readString(envFile, StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            content = content.replace("\r\n", "\n").replace('\r', '\n');
            for (String line : content.split("\n")) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                int eq = t.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String k = t.substring(0, eq).trim();
                String v = t.substring(eq + 1).trim();
                if (v.length() >= 2
                        && ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'")))) {
                    v = v.substring(1, v.length() - 1);
                }
                out.put(k, v);
            }
        } catch (IOException e) {
            log.warn("Could not read {}", envFile.toAbsolutePath(), e);
        }
        return out;
    }

    /**
     * JVM {@code user.dir} is not always the {@code cloudscanner} folder (IDE / Maven / monorepo).
     */
    static Path resolveDotenvDirectory() {
        Path cwd = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path[] envFiles = new Path[] {
                cwd.resolve(".env"),
                cwd.resolve("cloudscanner").resolve(".env"),
        };
        for (Path f : envFiles) {
            if (Files.isRegularFile(f)) {
                return f.getParent();
            }
        }
        Path parent = cwd.getParent();
        if (parent != null) {
            Path sibling = parent.resolve("cloudscanner").resolve(".env");
            if (Files.isRegularFile(sibling)) {
                return sibling.getParent();
            }
        }
        return cwd;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private static String stripQuotes(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.length() >= 2
                && ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'")))) {
            return t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider awsCredentialsProvider) {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsCredentialsProvider awsCredentialsProvider) {
        return S3Presigner.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }
}
