package dev.fusionize.storage.file;

import dev.fusionize.storage.StorageConfig;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileStorageServiceS3 implements FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceS3.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final FileStorageServiceLocal localFileSystemService;

    public FileStorageServiceS3(String awsAccessKey, String awsSecretKey,
            String bucketName, FileStorageServiceLocal localFileSystemService) {
        Region region = Region.US_EAST_1;
        if (bucketName.contains(":")) {
            String[] bucketNameParts = bucketName.split(":");
            this.bucketName = bucketNameParts[1];
            region = Region.regions().stream().filter(r -> r.id().equalsIgnoreCase(bucketNameParts[0]))
                    .findFirst().orElse(region);
        } else {
            this.bucketName = bucketName;
        }
        this.localFileSystemService = localFileSystemService;
        AwsCredentials awsCredentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
        s3Client = S3Client.builder().region(region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials)).build();
        logger.info("configured s3 bucket {} in region {}", this.bucketName, region);
    }

    @Override
    public Map<String, OutputStream> write(List<String> paths) throws IOException {
        return this.localFileSystemService.write(paths);
    }

    @Override
    public Map<String, InputStream> read(List<String> paths) {
        Map<String, InputStream> map = new HashMap<>();
        paths.forEach(p -> {
            try {
                InputStream is = s3Client.getObject(
                        GetObjectRequest.builder().bucket(this.bucketName).key(p).build());
                map.put(p, is);
            } catch (SdkClientException | S3Exception e) {
                logger.warn("s3 read failed for {}  while saving: {}", this.bucketName, e.getMessage());
                map.put(p, null);
            }
        });
        return map;
    }

    @Override
    public Map<String, Boolean> copy(Map<String, String> copyList) {
        Map<String, Boolean> result = new HashMap<>();
        copyList.keySet().forEach(source -> {
            try {
                CopyObjectRequest request = CopyObjectRequest.builder().destinationBucket(this.bucketName)
                        .sourceBucket(this.bucketName)
                        .sourceKey(source)
                        .destinationKey(copyList.get(source)).build();
                CopyObjectResponse response = s3Client.copyObject(request);
                result.put(source, response != null);
            } catch (SdkClientException | S3Exception e) {
                logger.warn("s3 copy failed for {}  while saving: {}", this.bucketName, e.getMessage());
                result.put(source, false);
            }
        });

        return result;
    }

    @Override
    public Map<String, Boolean> save(List<String> paths) {
        Map<String, Boolean> result = new HashMap<>();
        Map<String, InputStream> readMap = localFileSystemService.read(paths);
        readMap.keySet().forEach(key -> {
            try {
                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(this.bucketName)
                        .key(key)
                        .build();
                RequestBody body = RequestBody.fromBytes(IOUtils.toByteArray(readMap.get(key)));
                PutObjectResponse response = s3Client.putObject(request, body);
                result.put(key, response != null);
            } catch (SdkClientException | S3Exception | IOException e) {
                logger.warn("s3 save failed for {}  while saving: {}", this.bucketName, e.getMessage());
                result.put(key, false);
            }
        });
        List<String> toDelete = result.keySet().stream().filter(result::get).toList();
        localFileSystemService.remove(toDelete);
        return result;
    }

    @Override
    public Map<String, Boolean> remove(List<String> paths) {
        Map<String, Boolean> result = new HashMap<>();
        paths.forEach(p -> {
            try {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(this.bucketName).key(p).build();
                DeleteObjectResponse response = s3Client.deleteObject(deleteObjectRequest);
                result.put(p, response.deleteMarker());
            } catch (SdkClientException | S3Exception e) {
                logger.warn("s3 remove failed for {}  while saving: {}", this.bucketName, e.getMessage());
                result.put(p, false);
            }
        });
        return result;
    }

    @Override
    public void destroy() throws Exception {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    public static FileStorageServiceS3 instantiate(StorageConfig config,
                                                   FileStorageServiceLocal localFileSystemService) {
        String awsAccessKey = (String) config.getSecrets().get("accessKey");
        String awsSecretKey = (String) config.getSecrets().get("secretKey");
        String bucketName = (String) config.getProperties().get("bucket");

        return new FileStorageServiceS3(awsAccessKey, awsSecretKey, bucketName, localFileSystemService);
    }
}
