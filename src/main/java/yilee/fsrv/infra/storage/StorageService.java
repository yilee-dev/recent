package yilee.fsrv.infra.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yilee.fsrv.directory.folder.domain.entity.FolderObject;
import yilee.fsrv.infra.storage.exception.CreateStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;

@Component
@RequiredArgsConstructor
public class StorageService {
    private final StorageProps storageProps;

    public Path getRootPath() {
        return storageProps.getRoot().toAbsolutePath().normalize();
    }

    public List<String> getBreadcrumbs(FolderObject folderObject) {
        List<String> pathNames = new ArrayList<>();

        for (FolderObject current = folderObject; current != null; current = current.getParent()) {
            String name = current.getName();
            validateFolderName(name);
            pathNames.add(name);
        }

        Collections.reverse(pathNames);

        return pathNames;
    }

    public Path getFullPath(List<String> breadcrumbs) {
        Path path = getRootPath();

        for (String breadcrumb : breadcrumbs) {
            path = path.resolve(breadcrumb);
        }

        path = path.normalize();
        ensureUnderRoot(path);
        return path;
    }

    public Path resolveFolderPath(FolderObject folder) {
        return getFullPath(getBreadcrumbs(folder));
    }

    public String toRelative(Path absolute) {
        return getRootPath().relativize(absolute.normalize()).toString();
    }

    public void createFolder(FolderObject folder) {
        if (folder == null) return;

        Path dir = resolveFolderPath(folder);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new CreateStorageException("Folder create exception: " + dir, e);
        }
    }

    public String generateStoreName(String originalFilename) {
        String ext = extractExt(originalFilename);
        String base = UUID.randomUUID().toString();
        return (ext != null && !ext.isBlank()) ? base + "." + ext : base;
    }

    public StoredFile saveFileUnderFolder(FolderObject folder, InputStream in, String storeFilename) {
        createFolder(folder);
        Path folderPath = resolveFolderPath(folder);
        validateFileName(storeFilename);

        Path target = folderPath.resolve(storeFilename).normalize();
        ensureUnderRoot(target);

        long size;
        String sha256;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            try (DigestInputStream din = new DigestInputStream(in, md);
                 OutputStream out = Files.newOutputStream(target,
                         StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                size = copy(din, out);        // 파일 복사
            }

            sha256 = hex(md.digest());        // 🔹 digest 결과를 hex 문자열로 변환
        } catch (Exception e) {
            throw new CreateStorageException("File save exception", e);
        }

        return new StoredFile(target, toRelative(target), size, sha256);
    }

    private static long copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        long total = 0;
        int r;
        while ((r = in.read(buf)) != -1) {
            out.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    private static String extractExt(String name) {
        int p = (name == null) ? -1 : name.lastIndexOf('.');
        return (p > -1) ? name.substring(p + 1) : null;
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void validateFolderName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Folder name is empty");
        }

        if (name.contains("..") || name.matches(".*[\\\\/:*?\"<>|].*")) {
            throw new IllegalArgumentException("Invalid characters in folder name: " + name);
        }

        String upper = name.toUpperCase(Locale.ROOT);
        Set<String> reserved = Set.of("CON","PRN","AUX","NUL","COM1","LPT1");
        if (reserved.contains(upper)) {
            throw new IllegalArgumentException("Reserved name: " + name);
        }
    }
    private void validateFileName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("File name is empty");
        }
        if (name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new IllegalArgumentException("Invalid file name: " + name);
        }
    }

    private void ensureUnderRoot(Path path) {
        Path root = getRootPath();
        if (!path.startsWith(root)) {
            throw new SecurityException("Path traversal detected: " + path);
        }
    }

    public record StoredFile(Path absolutePath, String relativePath, long size, String sha256) {}
}
