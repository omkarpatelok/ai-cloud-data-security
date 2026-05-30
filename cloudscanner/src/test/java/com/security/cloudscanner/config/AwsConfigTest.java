package com.security.cloudscanner.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwsConfigTest {

    @Test
    void parseEnvFile_readsAccessKeysAndIgnoresComments(@TempDir Path dir) throws Exception {
        Path env = dir.resolve(".env");
        Files.writeString(env, """
                # comment
                AWS_ACCESS_KEY_ID=AKIA_TEST_KEY
                AWS_SECRET_ACCESS_KEY=secret/with=equals
                AWS_DEFAULT_REGION=ap-south-1
                """);

        Map<String, String> m = AwsConfig.parseEnvFile(env);
        assertEquals("AKIA_TEST_KEY", m.get("AWS_ACCESS_KEY_ID"));
        assertEquals("secret/with=equals", m.get("AWS_SECRET_ACCESS_KEY"));
        assertEquals("ap-south-1", m.get("AWS_DEFAULT_REGION"));
    }

    @Test
    void parseEnvFile_handlesCrOnlyLineEndings(@TempDir Path dir) throws Exception {
        Path env = dir.resolve(".env");
        Files.writeString(env, "AWS_ACCESS_KEY_ID=a\rAWS_SECRET_ACCESS_KEY=b\r");

        Map<String, String> m = AwsConfig.parseEnvFile(env);
        assertEquals("a", m.get("AWS_ACCESS_KEY_ID"));
        assertEquals("b", m.get("AWS_SECRET_ACCESS_KEY"));
    }

    @Test
    void resolveDotenvDirectory_returnsExistingDirectory() {
        Path resolved = AwsConfig.resolveDotenvDirectory();
        assertTrue(Files.isDirectory(resolved));
    }
}
