package io.intelliflow.dto.model;

public class DataModelProperty {

    private String name;

    private String type;

    private Boolean primitive = true;

    private String collectionType;

    private String valueType;

    private Boolean valueTypePrimitive = true;

    private Boolean mandatory = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getPrimitive() {
        return primitive;
    }

    public void setPrimitive(Boolean primitive) {
        this.primitive = primitive;
    }

    public String getValueType() {
        return valueType;
    }

    public String getCollectionType() {
        return collectionType;
    }

    public Boolean getValueTypePrimitive() {
        return valueTypePrimitive;
    }

    public void setCollectionType(String collectionType) {
        this.collectionType = collectionType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public void setValueTypePrimitive(Boolean valueTypePrimitive) {
        this.valueTypePrimitive = valueTypePrimitive;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
