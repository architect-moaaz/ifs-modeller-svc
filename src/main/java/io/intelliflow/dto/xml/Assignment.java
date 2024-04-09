package io.intelliflow.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Assignment {

    @JacksonXmlProperty(localName = "bpmn2:from")
    public From from;

    @JacksonXmlProperty(localName = "bpmn2:to")
    public To to;

    public Assignment(To to, From from) {
        this.to = to;
        this.from = from;
    }
}
