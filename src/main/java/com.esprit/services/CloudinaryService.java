// src/main/java/com/esprit/services/CloudinaryService.java
package com.esprit.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.esprit.utils.CloudinaryConfig;

import java.io.File;
import java.util.Map;

public class CloudinaryService {

    private final Cloudinary cloudinary = CloudinaryConfig.getInstance();

    /**
     * Upload un fichier PDF vers Cloudinary
     * @param pdfFile le fichier PDF local
     * @param publicId identifiant unique (ex: "factures/FAC-2026-010")
     * @return l'URL sécurisée du PDF sur Cloudinary
     */
    public String uploadPdf(File pdfFile, String publicId) throws Exception {
        Map<?, ?> result = cloudinary.uploader().upload(pdfFile, ObjectUtils.asMap(
                "resource_type", "raw",   // obligatoire pour les PDF
                "public_id",     publicId,
                "folder",        "factures",
                "overwrite",     true
        ));
        return (String) result.get("secure_url");
    }

    /**
     * Supprimer un PDF de Cloudinary
     */
    public void deletePdf(String publicId) throws Exception {
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                "resource_type", "raw"
        ));
    }
}