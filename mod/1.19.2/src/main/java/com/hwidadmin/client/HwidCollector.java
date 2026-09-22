package com.hwidadmin.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * Collects a stable, per-machine hardware identifier without any external
 * dependency (no OSHI/JNA).
 */
public final class HwidCollector {
    private static final String PROVIDER;
    private static final String VALUE;

    static {
        String v = null;
        String p = null;

        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win")) {
            p = "windows";
            v = readWindowsMachineGuid();
        } else if (os.contains("mac") || os.contains("darwin")) {
            p = "macos";
            v = readFirstNonEmpty(new String[]{
                    exec(new String[]{"ioreg", "-rd1", "-c", "IOPlatformExpertDevice"}),
                    exec(new String[]{"/usr/sbin/ioreg", "-rd1", "-c", "IOPlatformExpertDevice"})
            });
        } else {
            p = "linux";
            v = readFirstNonEmpty(new String[]{
                    readTrimmed("/etc/machine-id"),
                    readTrimmed("/var/lib/dbus/machine-id"),
                    exec(new String[]{"sh", "-c", "cat /sys/class/dmi/id/product_uuid 2>/dev/null"}),
                    exec(new String[]{"cat", "/sys/class/dmi/id/product_uuid"})
            });
        }

        if (v == null || v.isBlank()) {
            p = "fallback";
            v = "unknown-" + System.getProperty("user.name", "user");
        }

        PROVIDER = p;
        VALUE = v.trim();
    }

    private HwidCollector() {
    }

    /** Human-readable name of the backend used to collect the HWID. */
    public static String providerName() {
        return PROVIDER;
    }

    /** The raw hardware identifier (readable only to the local player). */
    public static String raw() {
        return VALUE;
    }

    /** Stable SHA-256 hash of the raw identifier, sent to the server. */
    public static String hash() {
        return sha256Hex("hwidadmin|" + PROVIDER + "|" + VALUE);
    }

    private static String readFirstNonEmpty(String[] candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c.trim();
            }
        }
        return null;
    }

    private static String readWindowsMachineGuid() {
        // reg query returns lines like:   MachineGuid    REG_SZ    {GUID}
        String out = exec(new String[]{"reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid"});
        if (out == null) {
            return null;
        }
        for (String line : out.split("\\r?\\n")) {
            if (line.contains("MachineGuid")) {
                int idx = line.indexOf("REG_SZ");
                if (idx >= 0) {
                    return line.substring(idx + "REG_SZ".length()).trim();
                }
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 0) {
                    return parts[parts.length - 1];
                }
            }
        }
        return null;
    }

    private static String exec(String[] cmd) {
        Process p = null;
        try {
            p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            if (!p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            String s = sb.toString().trim();
            return s.isEmpty() ? null : s;
        } catch (Exception e) {
            return null;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    private static String readTrimmed(String path) {
        try {
            String s = Files.readString(Path.of(path), StandardCharsets.UTF_8).trim();
            return s.isEmpty() ? null : s;
        } catch (Exception e) {
            return null;
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
