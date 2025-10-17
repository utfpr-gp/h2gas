package com.h2gas.controller;

import com.cloudinary.Cloudinary;
import com.h2gas.services.CloudinaryService;
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
            throw new RuntimeException(e);
        }
    }

}
