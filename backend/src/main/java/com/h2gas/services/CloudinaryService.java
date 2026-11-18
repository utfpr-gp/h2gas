package com.h2gas.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Component
public class CloudinaryService {
    @Autowired
    private Cloudinary cloudinary;

    public Map upload(MultipartFile file) throws IOException {
        return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
    }

    // HU28: endereço https da imagem (secure_url); "url" é http e o navegador bloqueia numa página https
    public static String secureUrl(Map<?, ?> uploaded) {
        Object secure = uploaded.get("secure_url");
        return String.valueOf(secure != null ? secure : uploaded.get("url"));
    }
}
