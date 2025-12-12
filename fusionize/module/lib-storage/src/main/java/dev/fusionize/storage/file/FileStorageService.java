package dev.fusionize.storage.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;

public interface FileStorageService extends DisposableBean {
    Map<String, OutputStream> write(List<String> paths) throws IOException;

    Map<String, InputStream> read(List<String> paths);

    Map<String, Boolean> copy(Map<String, String> copyList);

    Map<String, Boolean> save(List<String> paths);

    Map<String, Boolean> remove(List<String> paths);
}
