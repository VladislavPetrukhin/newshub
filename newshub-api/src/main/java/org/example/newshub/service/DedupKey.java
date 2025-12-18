package org.example.newshub.service;

import org.example.newshub.common.kafka.NewsItemPayload;
import org.example.newshub.model.NewsItem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class DedupKey {

    private DedupKey() {}

    static String of(NewsItem it) {
        String input = n(it.sourceId()) + "|" +
                n(it.guid()) + "|" +
                n(it.link()) + "|" +
                n(it.title()) + "|" +
                n(it.pubDateRaw());
        return sha256Hex(input);
    }

    static String of(NewsItemPayload it) {
        String input = n(it.sourceId()) + "|" +
                n(it.guid()) + "|" +
                n(it.link()) + "|" +
                n(it.title()) + "|" +
                n(it.pubDateRaw());
        return sha256Hex(input);
    }

    private static String sha256Hex(String input) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }

        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            String h = Integer.toHexString(b & 0xff);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }
}
