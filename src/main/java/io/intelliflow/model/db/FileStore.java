package io.intelliflow.model.db;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

@MongoEntity(collection = "filestore")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileStore {


    private String name;   // name of the file

    private int orderkey;  // order of partitions if split > 2MB

    private UUID id; // unique identifier for each data

    private ByteBuffer partition; // data as blob partitions

    private String status; // status of file

    private String appname; // appname to which the file is used

    private String user; // user maintaining the file

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileStore fileStore = (FileStore) o;
        return orderkey == fileStore.orderkey && Objects.equals(id, fileStore.id) && Objects.equals(name, fileStore.name) && Objects.equals(partition, fileStore.partition) && Objects.equals(status, fileStore.status) && Objects.equals(appname, fileStore.appname) && Objects.equals(user, fileStore.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, partition, orderkey, status, appname, user);
    }
}
