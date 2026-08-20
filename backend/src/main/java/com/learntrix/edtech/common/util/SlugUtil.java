package com.learntrix.edtech.common.util;

import java.util.UUID;

public final class SlugUtil {
    
    private SlugUtil() {}

    public static String generateSlug(String title) {
        if (title == null || title.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        
        String slug = title.toLowerCase().trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");

        return slug.isEmpty() ? UUID.randomUUID().toString() : slug;
    }
    
    public static String generateUniqueSlug(String title) {
        String base = generateSlug(title);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return base + "-" + suffix;
    }
}
