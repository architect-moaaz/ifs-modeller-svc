package io.intelliflow.dto.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

//@JacksonXmlRootElement(localName = "dataInputAssociation")
public class DataInputAssociation {

    @JacksonXmlProperty(localName = "bpmn2:sourceRef")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String sourceRef;

    @JacksonXmlProperty(localName = "bpmn2:assignment")
    @JacksonXmlElementWrapper
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Assignment assignment;

    @JacksonXmlProperty(localName = "bpmn2:targetRef")
    public String targetRef;

    public DataInputAssociation(String targetRef, Assignment assignment, String sourceRef) {
        this.targetRef = targetRef;
        this.assignment = assignment;
        this.sourceRef = sourceRef;
    }
}
