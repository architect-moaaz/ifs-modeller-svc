package io.intelliflow.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class DataInputXML {

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    public String id;
    @JacksonXmlProperty(isAttribute = true, localName = "drools:dtype")
    public String type;
    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String name;
    @JacksonXmlProperty(isAttribute = true, localName = "itemSubjectRef")
    public String itemSubjectRef;

    public DataInputXML(String id, String type, String name, String itemSubjectRef) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.itemSubjectRef = itemSubjectRef;
    }
}