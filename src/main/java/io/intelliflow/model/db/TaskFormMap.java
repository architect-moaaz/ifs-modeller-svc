package io.intelliflow.model.db;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@MongoEntity(collection = "filestore")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskFormMap {

    private ObjectId id;
    private String workspace;

    private String miniapp;

    private String taskid;

    private String taskname;

    private String tasktype;

    private String formname;

    private String bpmnname;
    public TaskFormMap(String workspace, String miniapp, String taskid, String taskname, String tasktype, String formname, String bpmnname) {
        this.workspace = workspace;
        this.miniapp = miniapp;
        this.taskid = taskid;
        this.taskname = taskname;
        this.tasktype = tasktype;
        this.formname = formname;
        this.bpmnname = bpmnname;
    }
}
