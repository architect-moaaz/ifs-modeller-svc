package io.intelliflow.repository;

import io.intelliflow.model.db.FileStore;
import io.quarkus.mongodb.panache.PanacheMongoRepository;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FileStoreRepository implements PanacheMongoRepository<FileStore> {

    public List<FileStore> getByFileNameAndId(UUID fileId, String fileName) {
        return find("fileId = ?1 and fileName = ?2", fileId, fileName).list();
    }
}
