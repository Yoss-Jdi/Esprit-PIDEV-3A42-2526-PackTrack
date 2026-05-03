package com.example.forumapp.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class PostImageStorage {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp");
    private static final Path ROOT_DIRECTORY = Paths.get(System.getProperty("user.home"), ".forumapp", "post-images")
            .toAbsolutePath()
            .normalize();

    private PostImageStorage() {}

    public static String importImage(File sourceFile) {
        if (sourceFile == null) return null;

        Path source = sourceFile.toPath().toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) throw new RuntimeException("Image introuvable.");

        String originalFileName = source.getFileName().toString();
        String extension = extractSupportedExtension(originalFileName);
        String baseName = sanitizeBaseName(originalFileName.substring(0, originalFileName.length() - extension.length()));
        String storedFileName = baseName + "-" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path target = ROOT_DIRECTORY.resolve(storedFileName).normalize();

        try {
            Files.createDirectories(ROOT_DIRECTORY);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'importer l'image.", e);
        }
    }

    public static void deleteStoredImage(String storedPath) {
        Path path = resolvePath(storedPath);
        if (path == null || !path.startsWith(ROOT_DIRECTORY)) return;

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de supprimer l'image enregistree.", e);
        }
    }

    public static String toExternalForm(String storedPath) {
        Path path = resolvePath(storedPath);
        return path != null && Files.exists(path) ? path.toUri().toString() : null;
    }

    public static String getDisplayName(String storedPath) {
        Path path = resolvePath(storedPath);
        return path == null ? "" : path.getFileName().toString();
    }

    private static Path resolvePath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return null;
        return Paths.get(storedPath).toAbsolutePath().normalize();
    }

    private static String extractSupportedExtension(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (lowerName.endsWith(extension)) return extension;
        }
        throw new RuntimeException("Format d'image non supporte. Utilisez PNG, JPG, JPEG, GIF, BMP ou WEBP.");
    }

    private static String sanitizeBaseName(String fileName) {
        String sanitized = fileName
                .trim()
                .replaceAll("[^a-zA-Z0-9-_]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return sanitized.isBlank() ? "image" : sanitized.toLowerCase(Locale.ROOT);
    }
}
