package com.example.book.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ImageStorageService {

	private final Path rootLocation = Paths.get("static/uploads");

    public String storeImage(MultipartFile file) throws Exception {
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path destinationFile = rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();
        if (!destinationFile.getParent().equals(rootLocation.toAbsolutePath())) {
            throw new Exception("No se puede almacenar el archivo fuera del directorio actual.");
        }
        try {
            if (file.isEmpty()) {
                throw new Exception("No se puede almacenar un archivo vacío.");
            }
            Files.copy(file.getInputStream(), destinationFile);
            return destinationFile.toString();
        } catch (Exception e) {
            throw new Exception("Error al almacenar la imagen: " + e.getMessage());
        }
    }
}
