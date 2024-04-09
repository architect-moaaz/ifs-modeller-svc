package io.intelliflow.model;

 /*
    @author rahul.malawadkar@intelliflow.ai
    @created on 25-07-2023
 */


import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class CustomFormDataDTO {

     @RestForm("file")
     private FileUpload file;

    @RestForm("workspace")
    private String workspace;

    @RestForm("appName")
    private String appName;

    @RestForm("fileName")
    private String fileName;

    @RestForm("fileType")
    private String fileType;

    public FileUpload getFile() {
        return file;
    }

    public void setFile(FileUpload file) {
        this.file = file;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public CustomFormDataDTO() {
    }

}
