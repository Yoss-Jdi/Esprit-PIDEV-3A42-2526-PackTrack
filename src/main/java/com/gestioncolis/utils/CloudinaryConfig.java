// src/main/java/com/esprit/utils/CloudinaryConfig.java
package com.gestioncolis.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

public class CloudinaryConfig {
    private static Cloudinary cloudinary;
    public static Cloudinary getInstance() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap( "cloud_name", "dqzaed1qj", "api_key", "439277141937143", "api_secret", "Vr4En2pzeOUCwuE5ulrUrzXaa7w" ));
        }
        return cloudinary;
    }
}