// src/main/java/com/esprit/utils/CloudinaryConfig.java
package com.esprit.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

public class CloudinaryConfig {

    private static Cloudinary cloudinary;

    public static Cloudinary getInstance() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "dqzaed1qj",
                    "api_key",    "463411945322135",
                    "api_secret", "8U941OyjLqaaMBd2BwMF9BHybuk"
            ));
        }
        return cloudinary;
    }
}