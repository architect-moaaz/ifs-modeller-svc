package io.intelliflow.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class OutputSet {
    @JacksonXmlProperty(localName = "bpmn2:dataOutputRefs")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> dataOutputRefs;

    public OutputSet(List<String> dataOutputRefs) {
        this.dataOutputRefs = dataOutputRefs;
    }
}
