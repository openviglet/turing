package com.viglet.turing.storage;

import com.viglet.turing.system.properties.TurStorageType;
import com.viglet.turing.storage.asset.TurAssetItem;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * Abstraction for object storage operations (MinIO, filesystem, or no-op).
 *
 * @author Alexandre Oliveira
 * @since 2026.2.4
 */
public interface TurStorageService extends com.viglet.turing.system.TurStorageStatus,
        com.viglet.turing.tenant.TurTenantObjectStore {

    @Override
    TurStorageType getType();

    @Override
    boolean isEnabled();

    List<TurAssetItem> listObjects(String prefix);

    List<TurAssetItem> listAllObjects();

    InputStream downloadObject(String objectName);

    TurStorageObjectStat statObject(String objectName);

    void uploadObject(MultipartFile file, String prefix);

    void createFolder(String folderPath);

    void uploadStream(String objectName, InputStream inputStream, long size, String contentType);

    @Override
    void deleteObjectsWithPrefix(String prefix);

    void deleteObject(String objectName);

    default String guessContentType(String name) {
        return TurStorageContentTypes.guessContentType(name);
    }
}
