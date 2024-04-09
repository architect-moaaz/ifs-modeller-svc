package io.intelliflow.dto.model;

public class FileModelDto extends BaseDataModelDTO {

    String fileId;

    String logoURL;

    String deviceSupport;

    String colorScheme;

    String description;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getLogoURL() {
        return logoURL;
    }

    public void setLogoURL(String logoURL) {
        this.logoURL = logoURL;
    }

    public String getDeviceSupport() {
        return deviceSupport;
    }

    public void setDeviceSupport(String deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    public String getColorScheme() {
        return colorScheme;
    }

    public void setColorScheme(String colorScheme) {
        this.colorScheme = colorScheme;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
