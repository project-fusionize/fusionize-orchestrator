package dev.fusionize.web.services;

import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.StorageConfigManager;
import dev.fusionize.storage.file.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileInboundConnectorServiceTest {

    private FileInboundConnectorService service;
    private StorageConfigManager configManager;

    @BeforeEach
    void setUp() {
        configManager = mock(StorageConfigManager.class);
        service = new FileInboundConnectorService(configManager);
    }

    // --- getFileStorageService ---

    @Test
    void getFileStorageService_returnsServiceWhenConfigExists() {
        StorageConfig config = new StorageConfig();
        FileStorageService storageService = mock(FileStorageService.class);
        when(configManager.getConfig("my-storage")).thenReturn(Optional.of(config));
        when(configManager.getFileStorageService(config)).thenReturn(storageService);

        FileStorageService result = service.getFileStorageService("my-storage");

        assertNotNull(result);
        assertSame(storageService, result);
    }

    @Test
    void getFileStorageService_returnsNullWhenConfigMissing() {
        when(configManager.getConfig("missing")).thenReturn(Optional.empty());

        FileStorageService result = service.getFileStorageService("missing");

        assertNull(result);
    }

    // --- listener management ---

    @Test
    void addAndInvokeListener() {
        var key = new FileInboundConnectorService.IngestKey("wf1", "node1");
        AtomicBoolean invoked = new AtomicBoolean(false);

        MultipartFile mockFile = mock(MultipartFile.class);
        service.addListener(key, file -> {
            invoked.set(true);
            assertSame(mockFile, file);
        });

        service.invoke(key, mockFile);
        assertTrue(invoked.get());
    }

    @Test
    void removeListener() {
        var key = new FileInboundConnectorService.IngestKey("wf1", "node1");
        AtomicBoolean invoked = new AtomicBoolean(false);

        service.addListener(key, file -> invoked.set(true));
        service.removeListener(key);
        service.invoke(key, mock(MultipartFile.class));

        assertFalse(invoked.get());
    }

    @Test
    void invokeUnknownKey_doesNotThrow() {
        var key = new FileInboundConnectorService.IngestKey("wf1", "node1");
        assertDoesNotThrow(() -> service.invoke(key, mock(MultipartFile.class)));
    }

    @Test
    void hasListener_returnsTrueWhenRegistered() {
        var key = new FileInboundConnectorService.IngestKey("wf1", "node1");
        service.addListener(key, file -> {});

        assertTrue(service.hasListener(key));
    }

    @Test
    void hasListener_returnsFalseWhenNotRegistered() {
        var key = new FileInboundConnectorService.IngestKey("wf1", "node1");
        assertFalse(service.hasListener(key));
    }

    @Test
    void hasListener_returnsFalseAfterRemoval() {
        var key = new FileInboundConnectorService.IngestKey("wf1", "node1");
        service.addListener(key, file -> {});
        service.removeListener(key);

        assertFalse(service.hasListener(key));
    }

    @Test
    void replaceListener_overwritesPrevious() {
        var key = new FileInboundConnectorService.IngestKey("wf1", "node1");
        AtomicReference<String> which = new AtomicReference<>("");

        service.addListener(key, file -> which.set("first"));
        service.addListener(key, file -> which.set("second"));
        service.invoke(key, mock(MultipartFile.class));

        assertEquals("second", which.get());
    }

    @Test
    void differentKeys_areIndependent() {
        var key1 = new FileInboundConnectorService.IngestKey("wf1", "node1");
        var key2 = new FileInboundConnectorService.IngestKey("wf1", "node2");
        AtomicBoolean invoked1 = new AtomicBoolean(false);
        AtomicBoolean invoked2 = new AtomicBoolean(false);

        service.addListener(key1, file -> invoked1.set(true));
        service.addListener(key2, file -> invoked2.set(true));
        service.invoke(key1, mock(MultipartFile.class));

        assertTrue(invoked1.get());
        assertFalse(invoked2.get());
    }

    @Test
    void keyEquality_worksCorrectly() {
        var key1 = new FileInboundConnectorService.IngestKey("wf1", "node1");
        var key2 = new FileInboundConnectorService.IngestKey("wf1", "node1");

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }
}
