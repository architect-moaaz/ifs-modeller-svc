package io.intelliflow.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class VariableDataAssociation {

    @JacksonXmlProperty(localName = "bpmn2:sourceRef")
    public String sourceRef;

    @JacksonXmlProperty(localName = "bpmn2:targetRef")
    public String targetRef;

    public VariableDataAssociation(String sourceRef, String targetRef) {
        this.sourceRef = sourceRef;
        this.targetRef = targetRef;
    }
}
