package io.intelliflow.repository;

import io.intelliflow.model.db.TaskFormMap;
import io.quarkus.mongodb.panache.PanacheMongoRepository;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class TaskFormMapRepository implements PanacheMongoRepository<TaskFormMap> {
    public List<TaskFormMap> findByTaskTypeAndName(String workspace, String miniapp, String tasktype, String taskname) {
        return find("workspace = ?1 and miniapp = ?2 and tasktype = 3? and taskname = 4?", workspace, miniapp,tasktype,taskname).list();
    }
}
