package com.financehub.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ExpenseCategoryImageStorage {

    private static final Set<String> ALLOWED_EXT = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp");
    private static final long MAX_ICON_BYTES = 250 * 1024;

    @Value("${fh.uploads.root:uploads}")
    private String uploadsRoot;

    public String store(MultipartFile file, long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_ICON_BYTES) {
            throw new IllegalArgumentException("Icon must be at most 250 KB.");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String ext = extension(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Icon must be PNG, JPEG, GIF, or WebP.");
        }
        Path dir = Path.of(uploadsRoot).toAbsolutePath().normalize()
                .resolve("expense-categories")
                .resolve(String.valueOf(userId));
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + ext;
        Path target = dir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/expense-categories/" + userId + "/" + filename;
    }

    public void deleteIfStored(String iconPath, long userId) {
        if (iconPath == null || !iconPath.startsWith("/uploads/expense-categories/")) {
            return;
        }
        try {
            String prefix = "/uploads/expense-categories/" + userId + "/";
            if (!iconPath.startsWith(prefix)) {
                return;
            }
            String relative = iconPath.substring("/uploads/".length());
            Path root = Path.of(uploadsRoot).toAbsolutePath().normalize();
            Path file = root.resolve(relative).normalize();
            if (file.startsWith(root)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    private static String extension(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0) {
            return "";
        }
        return filename.substring(i).toLowerCase(Locale.ROOT);
    }
}
