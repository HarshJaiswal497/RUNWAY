package com.runway.catalog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String saveFile(MultipartFile file, Long sellerId) throws IOException {
        // 1. Clean the filename to prevent directory traversal attacks
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        // 2. Generate a unique name to prevent overwriting
        // Example: 550e8400-e29b-iphone15.jpg
        String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;

        // 3. Define the seller-specific directory
        // Example: <upload-directory>/seller_101/
        Path sellerFolderPath = Paths.get(uploadDir).resolve("seller_" + sellerId);

        // 4. Create the directories if they don't exist
        if (!Files.exists(sellerFolderPath)) {
            Files.createDirectories(sellerFolderPath);
        }

        // 5. Copy the file to the target location
        Path targetLocation = sellerFolderPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // 6. Return the RELATIVE path for the database
        // Example: seller_101/550e8400-e29b-iphone15.jpg
        return "seller_" + sellerId + "/" + uniqueFileName;
    }

    public void deleteFile(String relativePath) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log the error but don't stop the product deletion
            System.err.println("Failed to delete file: " + relativePath);
        }
    }
}