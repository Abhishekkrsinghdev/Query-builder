package com.querybuilder.backend.shared.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility for generating URL-friendly slugs
 */
@Component
public class SlugGenerator {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-|-$)");
    private static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int RANDOM_SUFFIX_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generate slug from text
     * Example: "My Query Name" -> "my-query-name-a3b7f2"
     */
    public String generateSlug(String input) {
        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = EDGES_DASHES.matcher(slug).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH);

        // Add random suffix for uniqueness
        String suffix = generateRandomSuffix();

        return slug + "-" + suffix;
    }

    /**
     * Generate random alphanumeric suffix
     */
    private String generateRandomSuffix() {
        StringBuilder sb = new StringBuilder(RANDOM_SUFFIX_LENGTH);
        for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
            int index = random.nextInt(ALLOWED_CHARS.length());
            sb.append(ALLOWED_CHARS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Regenerate slug with new suffix
     */
    public String regenerateSlug(String existingSlug) {
        // Remove old suffix (last 7 characters: "-" + 6 chars)
        if (existingSlug.length() > 7) {
            String baseSlug = existingSlug.substring(0, existingSlug.length() - 7);
            return baseSlug + "-" + generateRandomSuffix();
        }
        return existingSlug + "-" + generateRandomSuffix();
    }
}