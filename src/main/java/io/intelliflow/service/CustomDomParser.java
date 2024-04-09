package io.intelliflow.service;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

public class CustomDomParser {

    public static NodeList getNodeList(File xmlFile, String localName, String namespaceURI) {
        String namespace;
        if(namespaceURI != null && namespaceURI != "") {
            namespace = namespaceURI;
        } else {
            namespace = "*";
        }
        try{
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputStream inputStream = new FileInputStream(xmlFile);
            Document document = documentBuilder.parse(inputStream);
            return document.getElementsByTagNameNS(namespace , localName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;
    }
}
