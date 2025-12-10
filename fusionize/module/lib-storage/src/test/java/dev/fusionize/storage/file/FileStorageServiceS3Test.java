package dev.fusionize.storage.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileStorageServiceS3Test {

    @Mock
    private S3Client s3Client;

    @Mock
    private FileStorageServiceLocal localFileSystemService;

    private FileStorageServiceS3 fileStorageServiceS3;
    private final String bucketName = "test-bucket";
    private final String region = "us-east-1";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // We need to inject the mock S3Client because it's created inside the
        fileStorageServiceS3 = new FileStorageServiceS3("key", "secret", region + ":" + bucketName,
                localFileSystemService);

        Field s3ClientField = FileStorageServiceS3.class.getDeclaredField("s3Client");
        s3ClientField.setAccessible(true);
        s3ClientField.set(fileStorageServiceS3, s3Client);
    }

    @Test
    void getStorageName() {
        assertEquals("s3:" + bucketName, fileStorageServiceS3.getStorageName());
    }

    @Test
    void read() {
        String key = "test-file.txt";
        String content = "hello world";
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(content.getBytes())));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        Map<String, InputStream> result = fileStorageServiceS3.read(Collections.singletonList(key));

        assertNotNull(result.get(key));
        try {
            assertEquals(content, new String(result.get(key).readAllBytes()));
        } catch (IOException e) {
            fail("IOException during read: " + e.getMessage());
        }
    }

    @Test
    void save() {
        String key = "test-file.txt";
        String content = "hello world";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        Map<String, InputStream> localReadMap = new HashMap<>();
        localReadMap.put(key, inputStream);

        when(localFileSystemService.read(anyList())).thenReturn(localReadMap);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        Map<String, Boolean> result = fileStorageServiceS3.save(Collections.singletonList(key));

        assertTrue(result.get(key));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(localFileSystemService, times(1)).remove(anyList());
    }

    @Test
    void remove() {
        String key = "test-file.txt";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().deleteMarker(true).build());

        Map<String, Boolean> result = fileStorageServiceS3.remove(Collections.singletonList(key));

        assertTrue(result.get(key));
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void copy() {
        String sourceKey = "source.txt";
        String destKey = "dest.txt";
        Map<String, String> copyMap = new HashMap<>();
        copyMap.put(sourceKey, destKey);

        when(s3Client.copyObject(any(CopyObjectRequest.class)))
                .thenReturn(CopyObjectResponse.builder().build());

        Map<String, Boolean> result = fileStorageServiceS3.copy(copyMap);

        assertTrue(result.get(sourceKey));
        verify(s3Client, times(1)).copyObject(any(CopyObjectRequest.class));
    }
}
