package io.intelliflow.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class IoSpecification {

    @JacksonXmlProperty(localName = "bpmn2:dataInput")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<DataInputXML> dataInput;

    @JacksonXmlProperty(localName = "bpmn2:dataOutput")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<DataInputXML> dataOutput;

    @JacksonXmlProperty(localName = "bpmn2:inputSet")
    @JacksonXmlElementWrapper
    public InputSet inputSet;

    @JacksonXmlProperty(localName = "bpmn2:outputSet")
    @JacksonXmlElementWrapper
    public OutputSet outputSet;

    public IoSpecification(List<DataInputXML> dataInput, List<DataInputXML> dataOutput, InputSet inputSet, OutputSet outputSet) {
        this.dataInput = dataInput;
        this.dataOutput = dataOutput;
        this.inputSet = inputSet;
        this.outputSet = outputSet;
    }
}
