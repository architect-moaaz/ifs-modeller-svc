package io.intelliflow.modeller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.intelliflow.dto.bindproperty.BasePropertyDto;
import io.intelliflow.dto.bindproperty.BindPropertyList;
import io.intelliflow.dto.model.*;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.dto.repomanager.FileInformation;
import io.intelliflow.dto.xml.*;
import io.intelliflow.service.FileOperations;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;


@Singleton
public class DMNModeller extends BaseModeller {

    @Override
    public void validate() {
        // TODO Auto-generated method stub
    }

    @Override
    public Uni<EventResponseModel> generateFile(BaseDataModelDTO baseDataModelDTO, String flag, String userId) {
        return null;
    }

    @Override
    public Uni<EventResponseModel> bind(BasePropertyDto basePropertyDto) {

        if (basePropertyDto.getFileType().equals("dmn")) {
            if(Objects.nonNull(basePropertyDto.getDataModelBinding()) && basePropertyDto.getDataModelBinding()) {
                    return bindDataModelToDmn(basePropertyDto);
            }else{
                return bindDmnToBpmn(basePropertyDto);
            }
        }else {
            return bindDrlToBpmn(basePropertyDto);
        }
    }

   private Uni<EventResponseModel> bindDataModelToDmn(BasePropertyDto basePropertyDto) {
    // ---------------------------------------------------------------------
       String classNameFromPackageName = getClassNameFromPackageName(basePropertyDto.getQualifiedObjectName());
//       Map<String, String>[] dataModelMetaData = new Map[]{new HashMap<>()};
    FileInformation fileInf = FileOperations.createFileInfoToFetch(basePropertyDto.getWorkspaceName(),
            basePropertyDto.getMiniAppName(),classNameFromPackageName ,"datamodel");

       // Fetching meta data of data model
    Uni<EventResponseModel> response = extensionService.fetchMetaContentFromRepository(fileInf);
    return response.onItem().transformToUni(iter ->{
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, String>> list = null;
        try {
            list = objectMapper.readValue(iter.getData().toString(), new TypeReference<List<Map<String, String>>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Map<String, String> map = new HashMap<>();
        for (Map<String, String> item1 : list) {
            map.put(item1.get("name"), dmnFileDataTypeConversion(item1.get("type")));
        }
//        Map<String, String> copyOfMap    = new HashMap<>();
//        copyOfMap.putAll(map);
//        dataModelMetaData[0] = copyOfMap;
//       Log.info("data model meta data = "+ map);
//        for(String key : dataModelMetaData[0].keySet()){
//            dataModelMetaData[0].put(key,dmnFileDataTypeConversion(dataModelMetaData[0].get(key)));
//        }
//        Log.info("data model meta data after conversion  = "+ dataModelMetaData[0]);
        // Fetch file from repo and update the local file
        FileInformation fileInfo = FileOperations.createFileInfoToFetch(basePropertyDto.getWorkspaceName(),
                basePropertyDto.getMiniAppName(), basePropertyDto.getDmnName(), "dmn");
        return extensionService.fetchFileFromRepository(fileInfo).onItem().transformToUni(
                item -> {
                    EventResponseModel responseModel = new EventResponseModel();
                    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    documentBuilderFactory.setNamespaceAware(true);
                    Document document = null;
                    try {
                        File tempFile = File.createTempFile("temp-", ".dmn");
                        Files.write(tempFile.toPath(), Collections.singleton(item.getData().toString()));
                        item.getData().toString();
                        InputStream inputStream = new FileInputStream(tempFile);

                        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                        document = documentBuilder.parse(inputStream);
                        Element root = document.getDocumentElement();
                        NodeList definitions;
                        if(basePropertyDto.getPropertyNames() ==null || basePropertyDto.getPropertyNames().isEmpty()) {
                            Log.info("mapping all fields ");
                            Element property = getItemDefinitionProperty(document,basePropertyDto.getId(),classNameFromPackageName);
                            for (String s : map.keySet()) {
                                basePropertyDto.setPropertyName(s);
                                Log.info("key = "+s+" value  = "+map.get(s));
                                String propertyName = Character.toLowerCase(basePropertyDto.getPropertyName().charAt(0)) +
                                        basePropertyDto.getPropertyName().substring(1);

                                definitions = document.getElementsByTagNameNS("*", "definitions");
                                boolean variableExists = false;
                                if (definitions.getLength() > 0) {
                                    Element definitions1 = (Element) definitions.item(0);
                                    NodeList itemComponentList = definitions1.getElementsByTagNameNS("*", "itemComponent");
                                    for (int i = 0; i < itemComponentList.getLength(); i++) {
                                        Element itemComponentElement = (Element) itemComponentList.item(i);
                                        if(itemComponentElement.getAttribute("name").equals(propertyName)){
                                            variableExists = true;
                                        }
                                    }
                                }
                                if (!variableExists) {
                                    Element property1 = document.createElement("dmn:itemComponent");
                                    property1.setAttribute("id","_" + UUID.randomUUID().toString().toUpperCase());

                                    property1.setAttribute("name",propertyName);
                                    property1.setAttribute("isCollection", "false");

                                    Element typeRef = document.createElement("dmn:typeRef");
                                    typeRef.setTextContent(map.get(s));
                                    property1.appendChild(typeRef);
                                    property.appendChild(property1);
                                    responseModel.setMessage("The binding was successfull");
                                } else {
                                    responseModel.setMessage("Fields Or Object already mapped");
                                }
                            }
                            //root.appendChild(property);
                            helperFunctionToAddPropertyInPosition(root,property);
                        }else{
                            Log.info("mapping selected fields.");

                                Element property = getItemDefinitionProperty(document, basePropertyDto.getId(), classNameFromPackageName);
                                for (String s : basePropertyDto.getPropertyNames()) {
                                    basePropertyDto.setPropertyName(s);
                                    String propertyName = Character.toLowerCase(basePropertyDto.getPropertyName().charAt(0)) +
                                            basePropertyDto.getPropertyName().substring(1);
                                    definitions = document.getElementsByTagNameNS("*", "definitions");
                                    boolean variableExists = false;
                                    if (definitions.getLength() > 0) {
                                        Element definitions1 = (Element) definitions.item(0);
                                        NodeList itemComponentList = definitions1.getElementsByTagNameNS("*", "itemComponent");
                                        for (int i = 0; i < itemComponentList.getLength(); i++) {
                                            Element itemComponentElement = (Element) itemComponentList.item(i);
                                            if(itemComponentElement.getAttribute("name").equals(propertyName)){
                                                variableExists = true;
                                            }
                                        }
                                    }

                                    if (!variableExists) {
                                        Element property1 = document.createElement("dmn:itemComponent");
                                        property1.setAttribute("id", "_" + UUID.randomUUID().toString().toUpperCase());

                                        property1.setAttribute("name", propertyName);
                                        property1.setAttribute("isCollection", "false");

                                        Element typeRef = document.createElement("dmn:typeRef");
                                        typeRef.setTextContent(map.get(s));
                                        property1.appendChild(typeRef);
                                        property.appendChild(property1);
                                        responseModel.setMessage("The binding was successfull");
                                    } else {
                                        responseModel.setMessage("Fields Or Object already mapped");
                                    }
                                }
//                                root.appendChild(property);
                                helperFunctionToAddPropertyInPosition(root,property);
                        }
                        this.writeXMLFile(document, tempFile);
                        fileInfo.setContent(Files.readString(tempFile.toPath()));
                        fileInfo.setComment("Model " + "propertyName" + " appended to DMN");
                        extensionService.updateFileInRepository(fileInfo).subscribe().with(
                                item1 -> Log.info("Updated DMN file in repository with updated mapping")
                        );
                        tempFile.delete();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException | ParserConfigurationException e) {
                        e.printStackTrace();
                    } catch (SAXException | TransformerException e) {
                        e.printStackTrace();
                        responseModel.setMessage("Cannot map the data model to empty dmn file.");
                    }

                    return Uni.createFrom().item(responseModel);
                }
        );
    });

}

    private void helperFunctionToAddPropertyInPosition(Element root, Element property) {
        NodeList childNodes = root.getChildNodes();
        int secondElementPosition = 1;
        int currentPosition = 0;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (currentPosition == secondElementPosition) {
                    // Insert the new element before the current second element
                    root.insertBefore(property, child);
                    break;
                }
                currentPosition++;
            }
        }
    }

    private String dmnFileDataTypeConversion(String s) {
        if(s.equals("String")){
            return "string";
        }else if(s.equals("Integer")){
            return "number";
        }else if(s.equals("Date")){
            return "date";
        }else if(s.equals("LocalDate")){
            return "date";
        }else if(s.equals("Long")){
            return "number";
        }else if(s.equals("LocalDateTime")){
            return "date and time";
        }else if(s.equals("Float")){
            return "number";
        }
        return "Any";
    }

    private Element getItemDefinitionProperty(Document document, String id, String classNameFromPackageName) {

        NodeList definitions = document.getElementsByTagNameNS("*", "definitions");
        NodeList processChildren = definitions.item(0).getChildNodes();
        boolean objectExists = false;
        for (int index = 0; index < processChildren.getLength(); index++) {
            //Verifying if data model with same name exists
            Node data = processChildren.item(index);

            if (Objects.nonNull(data.getAttributes()) &&
                    Objects.nonNull(data.getAttributes().getNamedItem("name")) &&
                    data.getAttributes().getNamedItem("name").getNodeValue().equals(classNameFromPackageName)) {
                objectExists = true;
            }
        }
        if(objectExists) {
            NodeList itemList = document.getElementsByTagName("dmn:itemDefinition");
            for (int i = 0; i < itemList.getLength(); i++) {
                Element it = (Element) itemList.item(i);
                String itemId = it.getAttribute("name");
                if (itemId.equals(classNameFromPackageName)) {
                    return it;
                }
            }
        }
            Element property = document.createElement("dmn:itemDefinition");
            property.setAttribute("id", "_" + UUID.randomUUID().toString().toUpperCase());
            property.setAttribute("name", classNameFromPackageName);
            property.setAttribute("isCollection", "false");
            return property;
    }

    private String getClassNameFromPackageName(String qualifiedObjectName) {
        String[] parts = qualifiedObjectName.split("\\.");
        return parts[parts.length - 1];
    }

    private Uni<EventResponseModel> bindDmnToBpmn(BasePropertyDto basePropertyDto) {
        FileInformation fetchFileInfo = FileOperations.createFileInfoToFetch(
                basePropertyDto.getWorkspaceName(),
                basePropertyDto.getMiniAppName(),
                basePropertyDto.getBpmnFileName(),
                "bpmn");
        Uni<EventResponseModel> response = extensionService.fetchFileFromRepository(fetchFileInfo);
        EventResponseModel responseModel = new EventResponseModel();
        return response.onItem()
                .transform(
                        item -> {
                            File tempFile = FileOperations.createTempFileBind(item, "bpmn");
                            try {
                                XmlMapper mapper = new XmlMapper();
                                List<DataInputXML> dataInputList = new ArrayList<>();
                                List<DataInputXML> dataOutputList = new ArrayList<>();
                                List<String> dataInputRefs = new ArrayList<>();
                                List<String> dataOutputRefs = new ArrayList<>();
                                List<DataInputAssociation> dataInputAssociationListList = new ArrayList<>();
                                List<VariableDataAssociation> varDataOutputAssocList = new ArrayList<>();
                                List<String> cases = new ArrayList<>() {
                                    {
                                        add("namespace");
                                        add("decision");
                                        add("model");
                                    }
                                };

                                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                                documentBuilderFactory.setNamespaceAware(true);
                                InputStream inputStream = new FileInputStream(tempFile);
                                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                                Document document = documentBuilder.parse(inputStream);

                                NodeList tasks = document.getElementsByTagNameNS("*", "businessRuleTask");
                                if(tasks.getLength() == 0){
                                    responseModel.setMessage("No Business Rule Found in BPMN");
                                    return responseModel;
                                }

                                for(int i = 0; i < tasks.getLength(); i++) {
                                    if(((Element)tasks.item(i)).getAttribute("name").equals(basePropertyDto.getBusinessRuleTaskName())) {
                                        String businessTaskId = ((Element)tasks.item(i)).getAttribute("id");
                                        NodeList childNodes = tasks.item(i).getChildNodes();
                                        for(int j = childNodes.getLength()-1 ; j >= 0 ; j--) {
                                            if(Objects.nonNull(childNodes.item(j).getLocalName()) &&
                                                    childNodes.item(j).getLocalName().equals("ioSpecification")) {
                                                NodeList ioSpecNodes = childNodes.item(j).getChildNodes();
                                                for(int k = 0; k < ioSpecNodes.getLength(); k++) {
                                                    if(Objects.nonNull(ioSpecNodes.item(k).getLocalName()) &&
                                                            ioSpecNodes.item(k).getLocalName().equals("dataInput")) {
                                                        Element dataInputElement = (Element) ioSpecNodes.item(k);
                                                        if(!Stream.of("namespace", "decision", "model").anyMatch(
                                                                dataInputElement.getAttribute("name")::equalsIgnoreCase)){
                                                            dataInputRefs.add(businessTaskId + "_" + dataInputElement.getAttribute("name") + "InputX");
                                                            dataInputAssociationListList.add(
                                                                    new DataInputAssociation(
                                                                            businessTaskId + "_" + dataInputElement.getAttribute("name") + "InputX",
                                                                            null,
                                                                            dataInputElement.getAttribute("name")
                                                                    )
                                                            );
                                                            dataInputList.add(new DataInputXML(
                                                                    businessTaskId + "_" + dataInputElement.getAttribute("name")  + "InputX",
                                                                    dataInputElement.getAttribute("drools:dtype"),
                                                                    dataInputElement.getAttribute("name"),
                                                                    basePropertyDto.getBusinessRuleTaskId() + "_" + dataInputElement.getAttribute("name") + "InputXItem"));
                                                        }
                                                        continue;
                                                    }
                                                    if(Objects.nonNull(ioSpecNodes.item(k).getLocalName()) &&
                                                            ioSpecNodes.item(k).getLocalName().equals("dataOutput")) {
                                                        Element dataInputElement = (Element) ioSpecNodes.item(k);
                                                        dataOutputRefs.add(businessTaskId + "_" + dataInputElement.getAttribute("name") + "OutputX");
                                                        varDataOutputAssocList.add(
                                                                new VariableDataAssociation(
                                                                        businessTaskId + "_" + dataInputElement.getAttribute("name") + "OutputX",
                                                                        dataInputElement.getAttribute("name")
                                                                )
                                                        );
                                                        dataOutputList.add(
                                                                new DataInputXML(
                                                                        businessTaskId + "_" + dataInputElement.getAttribute("name") + "OutputX",
                                                                        dataInputElement.getAttribute("drools:dtype"),
                                                                        dataInputElement.getAttribute("name"),
                                                                        basePropertyDto.getBusinessRuleTaskId() + "_" + ioSpecNodes.item(k).getAttributes().getNamedItem("name").getNodeValue() + "OutputXItem")
                                                        );
                                                    }
                                                }
                                            }
                                            if(Objects.nonNull(childNodes.item(j).getLocalName()) &&
                                                    Stream.of("ioSpecification", "dataInputAssociation", "dataOutputAssociation").anyMatch(
                                                            childNodes.item(j).getLocalName()::equalsIgnoreCase
                                                    )) {
                                                tasks.item(i).removeChild(childNodes.item(j));
                                            }
                                        }
                                    }
                                }
                               for (String current : cases) {
                                    To to = new To("bpmn2:tFormalExpression", basePropertyDto.getBusinessRuleTaskId() + "_" + current + "InputX");
                                    From from;
                                    if (current.equals("namespace")) {
                                        from = new From("bpmn2:tFormalExpression", basePropertyDto.getDmnNamespace());
                                    } else if (current.equals("decision")) {
                                        from = new From("bpmn2:tFormalExpression", basePropertyDto.getDecisionName());
                                    } else {
                                        from = new From("bpmn2:tFormalExpression", basePropertyDto.getDmnName());
                                    }
                                    Assignment assignment = new Assignment(to, from);
                                    dataInputAssociationListList.add(new DataInputAssociation(basePropertyDto.getBusinessRuleTaskId() + "_" + current + "InputX", assignment, null));
                                    dataInputList.add(new DataInputXML(basePropertyDto.getBusinessRuleTaskId() + "_" + current + "InputX", "java.lang.String", current, basePropertyDto.getBusinessRuleTaskId() + "_" + current + "InputXItem"));
                                    dataInputRefs.add(basePropertyDto.getBusinessRuleTaskId() + "_" + current + "InputX");

                                }

                                InputSet inputSet = new InputSet(dataInputRefs);
                                OutputSet outputSet = new OutputSet(dataOutputRefs);
                                IoSpecification ioSpecification = new IoSpecification(dataInputList, dataOutputList, inputSet, outputSet);
                                XMLRoot xmlSample = new XMLRoot(
                                        ioSpecification,
                                        dataInputAssociationListList,
                                        varDataOutputAssocList,
                                        "http://www.omg.org/spec/BPMN/20100524/MODEL",
                                        "http://www.jboss.org/drools",
                                        "http://www.w3.org/2001/XMLSchema-instance");
                                mapper.enable(SerializationFeature.INDENT_OUTPUT);

                                //write to xml

                                NodeList newDefinitions = document.getElementsByTagNameNS("*", "definitions");
                                Element newDefinition = (Element) newDefinitions.item(0);
                                newDefinition.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

                                Node task = tasks.item(0);
                                Element businessUpdate = (Element) tasks.item(0);
                                businessUpdate.setAttribute("implementation", "http://www.jboss.org/drools/dmn");


                                Document newDoc = documentBuilder.parse(new ByteArrayInputStream(mapper.writeValueAsString(xmlSample).getBytes()));
                                NodeList ioDefs = newDoc.getElementsByTagNameNS("*", "ioSpecification");
                                Node ioDef = ioDefs.item(0);
                                ioDef = document.importNode(ioDef, true);
                                task.appendChild(ioDef);
                                NodeList inputAssociations = newDoc.getElementsByTagNameNS("*", "dataInputAssociation");
                                for (int index = 0; index < inputAssociations.getLength(); index++) {
                                    Node inputAssociation = document.importNode(inputAssociations.item(index), true);
                                    task.appendChild(inputAssociation);
                                }

                                NodeList outputAssociations = newDoc.getElementsByTagNameNS("*", "dataOutputAssociation");
                                for (int index = 0; index < outputAssociations.getLength(); index++) {
                                    Node inputAssociation = document.importNode(outputAssociations.item(index), true);
                                    task.appendChild(inputAssociation);
                                }
                                writeXMLFile(document, tempFile);
                                fetchFileInfo.setComment("Updated BPMN with DMN");
                                fetchFileInfo.setContent(Files.readString(tempFile.toPath()));
                                extensionService.updateFileInRepository(fetchFileInfo).subscribe().with(
                                        item1 -> System.out.println("Success")
                                );
                                tempFile.delete();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (ParserConfigurationException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (TransformerException e) {
                                e.printStackTrace();
                            } catch (SAXException e) {
                                e.printStackTrace();
                            }
                            responseModel.setMessage("The binding was successfull");
                            return responseModel;
                        }
                );
    }

    private Uni<EventResponseModel> bindDrlToBpmn(BasePropertyDto basePropertyDto) {
        FileInformation fetchFileInfo = FileOperations.createFileInfoToFetch(
                basePropertyDto.getWorkspaceName(),
                basePropertyDto.getMiniAppName(),
                basePropertyDto.getBpmnFileName(),
                "bpmn");
        Uni<EventResponseModel> response = extensionService.fetchFileFromRepository(fetchFileInfo);
        return response.onItem()
                .transform(
                        item -> {
                            File tempFile = FileOperations.createTempFileBind(item, "bpmn");
                            try {
                                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                                documentBuilderFactory.setNamespaceAware(true);
                                InputStream inputStream = new FileInputStream(tempFile);
                                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                                Document document = documentBuilder.parse(inputStream);

                                NodeList tasks = document.getElementsByTagNameNS("*", "businessRuleTask");
                                Element businessUpdate = (Element) tasks.item(0);
                                businessUpdate.setAttribute("drools:ruleFlowGroup", basePropertyDto.getRuleFlowGroup());
                                writeXMLFile(document, tempFile);
                                fetchFileInfo.setComment("Updated BPMN with DRL");
                                fetchFileInfo.setContent(Files.readString(tempFile.toPath()));
                                extensionService.updateFileInRepository(fetchFileInfo).subscribe().with(
                                        item1 -> System.out.println("Success")
                                );
                                tempFile.delete();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (ParserConfigurationException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (SAXException e) {
                                e.printStackTrace();
                            } catch (TransformerException e) {
                                e.printStackTrace();
                            }
                            EventResponseModel responseModel = new EventResponseModel();
                            responseModel.setMessage("The binding was successfull");
                            return responseModel;
                        }
                );
    }

    //Method to write document to the XML BPMN file
    private void writeXMLFile(Document doc, File bpmnFile)
            throws TransformerFactoryConfigurationError, TransformerException {
        doc.getDocumentElement().normalize();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(bpmnFile);

        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

        transformer.transform(source, result);
        System.out.println("XML file updated successfully");
    }

    @Override
    public Uni<EventResponseModel> dataExtractor(FileModelDto fileModelDto) {
        FileInformation fileInfo = FileOperations.createFileInfoToFetch(fileModelDto.getWorkspaceName(),
                fileModelDto.getMiniAppName(), fileModelDto.getFileName(), "dmn");
        Uni<EventResponseModel> response = extensionService.fetchFileFromRepository(fileInfo);
        DMNResourceModel resourceModel = new DMNResourceModel();
        List<DecisionModel> decisionModels = new ArrayList<DecisionModel>();
                  return response.onItem()
                .transform(
                        item ->
                        {
                            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                            documentBuilderFactory.setNamespaceAware(true);
                            try {
                                File tempFile = FileOperations.createTempFileBind(item, "dmn");
                                InputStream inputStream = new FileInputStream(tempFile);
                                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                                Document document = documentBuilder.parse(inputStream);
                                //this function is returning NodeList
                                NodeList def = document.getElementsByTagNameNS("*", "definitions");
                                NodeList decisions = document.getElementsByTagNameNS("*", "decision");
                                for (int index = 0; index < decisions.getLength(); index++) {
                                    Node dec = decisions.item(index);
                                    if (dec.getNodeType() == Node.ELEMENT_NODE) {
                                        Element decElement = (Element) dec;
                                        DecisionModel decision = new DecisionModel();
                                         decision.setId(decElement.getAttribute("id"));
                                         decision.setName(decElement.getAttribute("name"));
                                         decisionModels.add(decision);
                                    }
                                }


                                Node definitions = def.item(0);
                                if (definitions.getNodeType() == Node.ELEMENT_NODE) {
                                    Element definitionsElement = (Element) definitions;
                                    resourceModel.setName(definitionsElement.getAttribute("name"));
                                    resourceModel.setNamespace(definitionsElement.getAttribute("namespace"));
                                }
                                tempFile.delete();

                            } catch (FileNotFoundException e) {

                                e.printStackTrace();
                            } catch (ParserConfigurationException e) {
                                throw new RuntimeException(e);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            } catch (SAXException e) {
                                throw new RuntimeException(e);
                            }
                            EventResponseModel responseModel = new EventResponseModel();
                            resourceModel.setDecisions(decisionModels);
                            responseModel.setData(resourceModel);
                           responseModel.setMessage("DMN Properties");
                            return responseModel;


                        }

                );
    }

    @Override
    public Uni<EventResponseModel> createMultiple(BaseModelList baseModels, String flag, String userId) {
        return null;
    }

    @Override
    public Uni<EventResponseModel> bindMultiple(BindPropertyList propertyList) {
        return null;
    }

}
