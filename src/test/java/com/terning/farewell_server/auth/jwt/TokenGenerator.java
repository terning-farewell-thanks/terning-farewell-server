package com.terning.farewell_server.auth.jwt;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TokenGenerator {
    public static void main(String[] args) throws Exception {
        String jwtSecretKey = System.getenv("JWT_SECRET_KEY");

        if (jwtSecretKey == null || jwtSecretKey.isBlank()) {
            System.err.println("오류: JWT_SECRET_KEY 환경 변수가 설정되지 않았습니다.");
            System.err.println("IntelliJ 실행 설정(Run Configuration)에 환경 변수를 추가해주세요.");
            return;
        }

        long jwtExpirationMs = 3600000;

        JwtUtil jwtUtil = new JwtUtil();

        setPrivateField(jwtUtil, "secretKeyString", jwtSecretKey);
        setPrivateField(jwtUtil, "expirationTime", jwtExpirationMs);

        jwtUtil.init();

        int userCount = 1500;
        List<String> tokens = new ArrayList<>();
        System.out.println("Generating " + userCount + " unique tokens...");

        for (int i = 1; i <= userCount; i++) {
            String email = "testuser_" + i + "@example.com";
            String token = jwtUtil.generateTemporaryToken(email);
            tokens.add(token);
        }

        writeTokensToFile(tokens);

        System.out.println("Successfully created 'performance/tokens.txt' file with " + userCount + " tokens.");
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void writeTokensToFile(List<String> tokens) throws IOException {
        Path performanceDirPath = Paths.get("performance");
        if (!Files.exists(performanceDirPath)) {
            Files.createDirectories(performanceDirPath);
        }
        Path filePath = performanceDirPath.resolve("tokens.txt");
        Files.write(filePath, tokens);
    }
}
