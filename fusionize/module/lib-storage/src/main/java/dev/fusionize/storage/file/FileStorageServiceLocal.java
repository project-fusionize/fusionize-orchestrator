package dev.fusionize.storage.file;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileStorageServiceLocal implements FileStorageService {
    private static class FileSystemPackage {
        private File file;
        private final String filePath;
        private Path path;

        private FileSystemPackage(String filePath) {
            this.filePath = filePath;
        }
    }

    private final Path fileStorageLocation;

    public FileStorageServiceLocal(String filePath) throws IOException {
        if (filePath == null)
            filePath = "";

        this.fileStorageLocation = Paths.get(filePath)
                .toAbsolutePath().normalize();

        Files.createDirectories(this.fileStorageLocation);
    }

    private void loadFileSystemPackage(FileSystemPackage f) {
        f.path = this.fileStorageLocation.resolve(f.filePath).normalize();
        f.file = new File(f.path.toString());
    }

    @Override
    public Map<String, OutputStream> write(List<String> paths) throws IOException {
        List<FileSystemPackage> files = paths.stream()
                .map(FileSystemPackage::new)
                .peek(this::loadFileSystemPackage)
                .toList();

        Map<String, OutputStream> map = new HashMap<>();
        for (FileSystemPackage f : files) {
            String parentFolder = f.file.getParent();
            Files.createDirectories(Path.of(parentFolder));
            if (!f.file.exists()) {
                f.file.createNewFile();
            }
            map.put(f.filePath, new FileOutputStream(f.file));
        }
        return map;
    }

    @Override
    public Map<String, InputStream> read(List<String> paths) {
        Map<String, InputStream> map = new HashMap<>();
        paths.stream()
                .map(FileSystemPackage::new)
                .peek(this::loadFileSystemPackage)
                .forEach(f -> {
                    try {
                        map.put(f.filePath, f.path.toUri().toURL().openStream());
                    } catch (IOException e) {
                        map.put(f.filePath, null);
                    }
                });
        return map;
    }

    @Override
    public Map<String, Boolean> copy(Map<String, String> copyList) {
        Map<String, Boolean> map = new HashMap<>();
        copyList.keySet().stream()
                .map(FileSystemPackage::new)
                .peek(this::loadFileSystemPackage)
                .forEach(f -> {
                    try {
                        FileSystemPackage target = new FileSystemPackage(copyList.get(f.filePath));
                        this.loadFileSystemPackage(target);
                        String parentFolder = target.file.getParent();
                        Files.createDirectories(Path.of(parentFolder));
                        Files.copy(f.path, target.path);
                        map.put(f.filePath, true);
                    } catch (IOException e) {
                        map.put(f.filePath, null);
                    }
                });
        return map;
    }

    @Override
    public Map<String, Boolean> save(List<String> paths) {
        Map<String, Boolean> result = new HashMap<>();
        paths.forEach(p -> result.put(p, true));
        return result;
    }

    @Override
    public Map<String, Boolean> remove(List<String> paths) {
        Map<String, Boolean> result = new HashMap<>();
        paths.stream()
                .map(FileSystemPackage::new)
                .peek(f -> f.path = this.fileStorageLocation.resolve(f.filePath))
                .peek(p -> p.file = new File(p.path.toString()))
                .filter(p -> p.file.exists())
                .forEach(p -> result.put(p.filePath, p.file.delete()));
        return result;
    }

    @Override
    public void destroy() throws Exception {
        // No resources to close
    }
}
