package io.intelliflow.dto.model.db;

import java.util.UUID;

/*
    Data Transfer Object to obtain data from API
    and mapped to entity for data manipulation
 */
public class FileStoreDto {

    private UUID id;

    private String fileName;

    private String name;

    private String appName;

    private String user;

    private String format;

    // setting default value as Pending
    private String status = "Pending";

    private String content;

    public FileStoreDto() {
    }

    public FileStoreDto(UUID id, String fileName, String name, String appName, String user, String format, String status, String content) {
        this.id = id;
        this.fileName = fileName;
        this.name = name;
        this.appName = appName;
        this.user = user;
        this.format = format;
        this.status = status;
        this.content = content;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
