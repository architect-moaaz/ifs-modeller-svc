package io.intelliflow.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class InputSet {
    @JacksonXmlProperty(localName = "bpmn2:dataInputRefs")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> dataInputRefs;

    public InputSet(List<String> dataInputRefs) {
        this.dataInputRefs = dataInputRefs;
    }
}
