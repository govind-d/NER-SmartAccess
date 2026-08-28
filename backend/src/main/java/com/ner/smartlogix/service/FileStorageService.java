package com.ner.smartlogix.service;

import com.ner.smartlogix.exception.BusinessRuleException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores incident photos on disk.
 *
 * <p>Images are never put in the database: a few hundred kilobytes per row would bloat
 * every backup and every query plan for no benefit.
 *
 * <p>Three defences against a malicious upload:
 * <ol>
 *   <li>the declared content type must be an image;</li>
 *   <li>the first bytes of the file must actually match a known image format, because a
 *       content type is just a claim the client makes;</li>
 *   <li>the stored name is generated, so a name like {@code ../../application.yml} can
 *       never escape the upload folder.</li>
 * </ol>
 */
@Slf4j
@Service
public class FileStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png", "image/webp");

    @Value("${app.storage.path}")
    private String storagePath;

    private Path root;

    @PostConstruct
    void init() {
        try {
            root = Paths.get(storagePath).toAbsolutePath().normalize();
            Files.createDirectories(root);
            log.info("Photo storage ready at {}", root);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create the upload folder: " + storagePath, ex);
        }
    }

    /** Saves the file and returns the generated file name to store on the entity. */
    public String store(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleException("Photo must be 5 MB or smaller");
        }
        if (file.getContentType() == null || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessRuleException("Photo must be a JPEG, PNG or WebP image");
        }

        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (!looksLikeImage(header)) {
                throw new BusinessRuleException("The uploaded file is not a real image");
            }
        } catch (IOException ex) {
            throw new BusinessRuleException("Could not read the uploaded file");
        }

        String extension = switch (file.getContentType()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String fileName = prefix + "-" + UUID.randomUUID() + extension;

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, root.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessRuleException("Could not save the photo");
        }
        return fileName;
    }

    public Resource load(String fileName) {
        try {
            Path file = root.resolve(fileName).normalize();
            // Defence in depth: refuse anything that resolved outside the upload folder.
            if (!file.startsWith(root)) {
                throw new BusinessRuleException("Invalid file name");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessRuleException("Photo not found");
            }
            return resource;
        } catch (IOException ex) {
            throw new BusinessRuleException("Photo not found");
        }
    }

    /** Magic bytes: JPEG starts FF D8 FF, PNG starts 89 P N G, WebP is RIFF....WEBP. */
    private boolean looksLikeImage(byte[] header) {
        if (header.length < 12) {
            return false;
        }
        boolean jpeg = (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
        boolean png = (header[0] & 0xFF) == 0x89 && header[1] == 'P'
                && header[2] == 'N' && header[3] == 'G';
        boolean webp = header[0] == 'R' && header[1] == 'I' && header[2] == 'F'
                && header[3] == 'F' && header[8] == 'W' && header[9] == 'E'
                && header[10] == 'B' && header[11] == 'P';
        return jpeg || png || webp;
    }
}
