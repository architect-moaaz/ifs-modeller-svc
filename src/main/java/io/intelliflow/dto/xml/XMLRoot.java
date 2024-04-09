package io.intelliflow.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement
public class XMLRoot {

    @JacksonXmlProperty(localName = "bpmn2:ioSpecification")
    @JacksonXmlElementWrapper
    public IoSpecification ioSpecification;

    @JacksonXmlProperty(localName = "bpmn2:dataInputAssociation")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<DataInputAssociation> dataInputAssociation;

    @JacksonXmlProperty(localName = "bpmn2:dataOutputAssociation")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<VariableDataAssociation> varDataOutputAssociation;

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:bpmn2")
    public String bpmn2;

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:drools")
    public String drools;

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
    public String xsi;


    public XMLRoot(IoSpecification ioSpecification, List<DataInputAssociation> dataInputAssociation, List<VariableDataAssociation> varDataOutputAssociation, String bpmn2, String drools, String xsi) {
        this.ioSpecification = ioSpecification;
        this.dataInputAssociation = dataInputAssociation;
        this.varDataOutputAssociation = varDataOutputAssociation;
        this.bpmn2 = bpmn2;
        this.drools = drools;
        this.xsi = xsi;
    }
}

