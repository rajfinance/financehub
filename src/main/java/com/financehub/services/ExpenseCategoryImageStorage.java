package com.financehub.services;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
public class ExpenseCategoryImageStorage {

    private static final Set<String> ALLOWED_EXT = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp");
    private static final long MAX_ICON_BYTES = 250 * 1024;

    public record StoredIcon(byte[] data, String contentType) {}

    public StoredIcon readValidated(MultipartFile file) throws IOException {
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
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank() || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            contentType = contentTypeForExt(ext);
        }
        return new StoredIcon(file.getBytes(), contentType);
    }

    private static String contentTypeForExt(String ext) {
        return switch (ext) {
            case ".png" -> MediaType.IMAGE_PNG_VALUE;
            case ".gif" -> MediaType.IMAGE_GIF_VALUE;
            case ".webp" -> "image/webp";
            default -> MediaType.IMAGE_JPEG_VALUE;
        };
    }

    private static String extension(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0) {
            return "";
        }
        return filename.substring(i).toLowerCase(Locale.ROOT);
    }
}
