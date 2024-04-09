package io.intelliflow.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public class To {

    @JacksonXmlProperty(isAttribute = true, localName = "xsi:type")
    public String type;

    @JacksonXmlCData
    @JacksonXmlText
    public String cdataText;

    public To(String type, String cdataText) {
        this.type = type;
        this.cdataText = cdataText;
    }
}
