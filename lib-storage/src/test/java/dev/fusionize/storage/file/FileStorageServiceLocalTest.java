package dev.fusionize.storage.file;

import dev.fusionize.common.utility.KeyUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceLocalTest {

    FileStorageServiceLocal fileStorageService;
    static final String LOCATION = "/tmp/myFileSystem";
    static final String STAGING = "/tmp/stagingRoot";

    Map<String, String> content = new HashMap<>();


    @BeforeEach
    void setUp() throws IOException {
        File root = new File(LOCATION);
        File staging = new File(STAGING);

        if (root.exists()) {
            FileUtils.forceDelete(root);
        }
        if (staging.exists()) {
            FileUtils.forceDelete(staging);
        }

        fileStorageService = new FileStorageServiceLocal(LOCATION);
    }

    @Test
    void write() throws IOException {
        List<String> files = new ArrayList<>();
        createFiles(files, STAGING, 3);
        System.out.println(files);
        Map<String, String> sourceTarget = new HashMap<>();
        files.forEach(f -> sourceTarget.put(f.replace(STAGING + "/", ""), f));
        Map<String, OutputStream> outputStreamMap = fileStorageService.write(new ArrayList<>(sourceTarget.keySet()));
        outputStreamMap.keySet().forEach(k -> {
            try {
                outputStreamMap.get(k).write(FileUtils.readFileToString(new File(sourceTarget.get(k)), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8));
                outputStreamMap.get(k).flush();
                outputStreamMap.get(k).close();
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });

        outputStreamMap.keySet().forEach(k -> {
            File refFile = new File(LOCATION + "/" + k);
            String fileContent = null;
            try {
                fileContent = FileUtils.readFileToString(refFile, StandardCharsets.UTF_8);
                content.put(k, fileContent);
                assertTrue(refFile.exists());
                System.out.println(k);
                System.out.println(refFile.getAbsolutePath());
                System.out.println(fileContent);
            } catch (IOException e) {
                fail(e.getMessage());
            }

        });

    }

    @Test
    void read() throws IOException {
        write();
        Map<String, InputStream> inputStreamMap = fileStorageService.read(new ArrayList<>(content.keySet()));
        inputStreamMap.keySet().forEach(key -> {
            byte[] bytes;
            try {
                bytes = IOUtils.toByteArray(inputStreamMap.get(key));
                String retrieveContent = new String(bytes, StandardCharsets.UTF_8);
                assertEquals(content.get(key), retrieveContent);
                System.out.println(retrieveContent);
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    void remove() throws IOException {
        write();

        Map<String, Boolean> result = fileStorageService.remove(new ArrayList<>(content.keySet()));
        assertTrue(result.values().stream().allMatch(r -> r));
        File root = new File(LOCATION);
        assertTrue(root.exists());
        if (root.exists()) {
            content.keySet().forEach(f -> {
                assertFalse(new File(f).exists());
            });
        }
    }

    @Test
    void copy() throws IOException {
        write();

        Map<String, String> copyList = new HashMap<>();
        content.keySet().forEach(k -> copyList.put(k, "copied/" + k));
        Map<String, Boolean> result = fileStorageService.copy(copyList);
        assertTrue(result.values().stream().allMatch(r -> r));
        Map<String, InputStream> inputStreamMap = fileStorageService.read(content.keySet().stream().map(k ->
                "copied/" + k).collect(Collectors.toList()));
        inputStreamMap.keySet().forEach(key -> {
            byte[] bytes;
            try {
                bytes = IOUtils.toByteArray(inputStreamMap.get(key));
                String retrieveContent = new String(bytes, StandardCharsets.UTF_8);
                assertEquals(content.get(key.replace("copied/", "")), retrieveContent);
                System.out.println(retrieveContent);
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
    }

    public static void createFiles(List<String> files, String root, int iteration) throws IOException {
        if (!root.endsWith("/")) {
            root = root + "/";
        }
        for (int i = 0; i < iteration; i++) {
            String fn = KeyUtil.getRandomAlphabeticalKey(8);
            boolean directory = i % 2 == 0;
            if (directory && iteration > 1) {
                String dir = root + fn + "/";
                Files.createDirectories(Path.of(dir));
                createFiles(files, dir, iteration - 1);
            } else {
                String file = root + fn + ".text";
                String content = KeyUtil.getHash(fn);
                FileUtils.writeStringToFile(new File(file), content, StandardCharsets.UTF_8);
                files.add(file);
            }

        }
    }
}