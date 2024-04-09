package io.intelliflow.dto.model;

import java.util.List;

public class DataModelDto extends BaseDataModelDTO {

    private List<DataModelProperty> dataModelProperties;

    public List<DataModelProperty> getDataModelProperties() {
        return dataModelProperties;
    }

    public void setDataModelProperties(List<DataModelProperty> dataModelProperties) {
        this.dataModelProperties = dataModelProperties;
    }
}
