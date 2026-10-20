package com.h2gas.controller;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.cloudinary.Cloudinary;
import com.h2gas.services.CloudinaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Tag(name = "Upload")
@RestController
@RequestMapping("/api/upload")
public class UploadController {
    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping
    @RolesAllowed("ADMIN")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Map result = cloudinaryService.upload(file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível enviar a imagem. Tente novamente.", e);
        }
    }

}
