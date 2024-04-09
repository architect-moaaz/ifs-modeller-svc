package io.intelliflow.dto.model;

import javax.validation.constraints.NotBlank;

public abstract class BaseDataModelDTO {
	
    @NotBlank
    private String workspaceName;

    @NotBlank
    private String miniAppName;

    @NotBlank
    private String fileName;

    private byte[] fileContent;

    private String fileType;
    
    private String comment;

    private String updatedName;


    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getMiniAppName() {
        return miniAppName;
    }

    public void setMiniAppName(String miniAppName) {
        this.miniAppName = miniAppName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

    public String getUpdatedName() {
        return updatedName;
    }

    public void setUpdatedName(String updatedName) {
        this.updatedName = updatedName;
    }
}
