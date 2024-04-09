package io.intelliflow.modeller;

import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.intelliflow.centralCustomExceptionHandler.CustomException;
import io.intelliflow.dto.bindproperty.BasePropertyDto;
import io.intelliflow.dto.bindproperty.BindPropertyList;
import io.intelliflow.dto.model.*;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.dto.repomanager.FileInformation;
import io.intelliflow.service.FileOperations;
import io.smallrye.mutiny.Uni;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.util.*;


@Singleton
public class BPMNModeller extends BaseModeller {

    @Override
    public void validate() {
        // TODO Auto-generated method stub
    }

    @Override
    public Uni<EventResponseModel> generateFile(BaseDataModelDTO baseDataModelDTO, String flag, String userId) {
        return null;
    }

    public Uni<EventResponseModel> createFile(BaseDataModelDTO baseModelDto, String userId) throws CustomException {

        FileInformation fileInfo = new FileInformation();
        fileInfo.setWorkspaceName(baseModelDto.getWorkspaceName());
        fileInfo.setMiniApp(baseModelDto.getMiniAppName());
        fileInfo.setFileName(baseModelDto.getFileName() + ".bpmn");
        fileInfo.setFileType(baseModelDto.getFileType());
        fileInfo.setUserId(userId);
        if(baseModelDto.getComment() != null) {
            fileInfo.setComment(baseModelDto.getComment());
        } else {
            fileInfo.setComment("Created " + baseModelDto.getFileName() + " in Workspace");
        }
        if(baseModelDto.getFileContent() != null) {
            fileInfo.setContent(new String(baseModelDto.getFileContent()));
            FileInformation metaFile = FileOperations.createFileInfoForMeta(baseModelDto);
            extensionService.createMetaFileInWorkspace(metaFile).subscribe()
                    .with(
                            item -> System.out.println("Meta Created for BPMN")
                    );
            List<String> startMessage = new ArrayList<>();
        List<String> endMessage = new ArrayList<>();

        try {
            InputStream inputStream = new ByteArrayInputStream(baseModelDto.getFileContent());

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(inputStream);
            NodeList messages = document.getElementsByTagNameNS("*" , "message");
            if(messages.getLength() > 0) {
                //If there are messages present
                NodeList msgEventDefs = document.getElementsByTagNameNS("*" , "messageEventDefinition");
                for( int index = 0 ; index < msgEventDefs.getLength() ; index++){
                    Node msgEventDef = msgEventDefs.item(index);
                    if(msgEventDef.getParentNode().getLocalName().equalsIgnoreCase("startEvent")) {
                        startMessage.add(msgEventDef.getAttributes().getNamedItem("drools:msgref").getNodeValue());
                    }
                    //Enable when end messages are supported
                    else {
                        endMessage.add(msgEventDef.getAttributes().getNamedItem("drools:msgref").getNodeValue());
                    }
                }
            }
            MessageEventModel msgModel = new MessageEventModel();
            msgModel.setEndMessages(endMessage);
            msgModel.setStartMessages(startMessage);
            msgModel.setWorkspacename(baseModelDto.getWorkspaceName());
            msgModel.setAppname(baseModelDto.getMiniAppName());
            msgModel.setBpmnName(baseModelDto.getFileName());
            return extensionService.updatePropertiesForMessage(msgModel).onItem().transformToUni(
                    item -> extensionService.createFileInRepo(fileInfo).onItem()
                            .transformToUni(
                                    iter -> {
                                        EventResponseModel responseModel = new EventResponseModel();
                                        responseModel.setMessage("File Created");
                                        return Uni.createFrom().item(responseModel);
                                    }
                            )
                    );


        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
        } else {
            return extensionService.createFileInRepo(fileInfo).onItem()
                    .transformToUni(
                            iter -> {
                                EventResponseModel responseModel = new EventResponseModel();
                                responseModel.setMessage("File Created");
                                return Uni.createFrom().item(responseModel);
                            }
                    );
        }
    }

    @Override
    public Uni<EventResponseModel> bind(BasePropertyDto basePropertyDto) {
        // ---------------------------------------------------------------------
        // Fetch file from repo and update the local file
        FileInformation fileInfo = FileOperations.createFileInfoToFetch(basePropertyDto.getWorkspaceName(),
                basePropertyDto.getMiniAppName(), basePropertyDto.getBpmnFileName(),"bpmn");
        Uni<EventResponseModel> response = extensionService.fetchFileFromRepository(fileInfo);
        return response.onItem()
                .transform(
                        item -> {
                            EventResponseModel responseModel = new EventResponseModel();
                            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                            documentBuilderFactory.setNamespaceAware(true);
                            try{
                                File tempFile = File.createTempFile("temp-",".bpmn");
                                Files.write(tempFile.toPath(), Collections.singleton(item.getData().toString()));
                                InputStream inputStream = new FileInputStream(tempFile);
                                String propertyName = Character.toLowerCase(basePropertyDto.getPropertyName().charAt(0)) +
                                        basePropertyDto.getPropertyName().substring(1);

                                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

                                Document document = documentBuilder.parse(inputStream);

                                NodeList definitions = document.getElementsByTagNameNS("*" , "definitions");
                                NodeList processes = document.getElementsByTagNameNS("*" , "process");
                                NodeList processChildren = processes.item(0).getChildNodes();
                                boolean variableExists = false;
                                for( int index = 0 ; index < processChildren.getLength() ; index++){
                                    //Verifying if data model with same name exists
                                    Node data = processChildren.item(index);

                                    if(Objects.nonNull(data.getAttributes()) &&
                                            Objects.nonNull(data.getAttributes().getNamedItem("name")) &&
                                            data.getAttributes().getNamedItem("name").getNodeValue().equals(propertyName)){
                                        variableExists = true;
                                    }
                                }

                                if(!variableExists) {
                                    Node process = processes.item(0);
                                    Element property = document.createElement( "bpmn2:property");

                                    property.setAttribute("id", propertyName);
                                    property.setAttribute("name", propertyName);
                                    property.setAttribute("itemSubjectRef", basePropertyDto.getId());


                                    process.appendChild(property);

                                    Node definition = definitions.item(0);

                                    Element itemDefinition = document.createElement( "bpmn2:itemDefinition");

                                    itemDefinition.setAttribute("id", basePropertyDto.getId());
                                    itemDefinition.setAttribute("structureRef", basePropertyDto.getQualifiedObjectName());


                                    definition.appendChild(itemDefinition);

                                    this.writeXMLFile(document , tempFile);
                                    fileInfo.setContent(Files.readString(tempFile.toPath()));
                                    fileInfo.setComment("Model " + propertyName + " appended to BPMN");
                                    extensionService.updateFileInRepository(fileInfo).subscribe().with(
                                            item1 -> System.out.println("Binded " + propertyName + " to BPMN")
                                    );
                                    responseModel.setMessage("The binding was successfull");
                                } else {
                                    responseModel.setMessage("Object for Model with same name already exists");
                                }


                                tempFile.delete();

                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException | ParserConfigurationException e) {
                                e.printStackTrace();
                            }  catch (SAXException |TransformerException e) {
                                e.printStackTrace();
                            }
                            return responseModel;
                        }
                );
    }

    @Override
    public Uni<EventResponseModel> dataExtractor(FileModelDto fileModelDto) {
        FileInformation fileInfo = FileOperations.createFileInfoToFetch(fileModelDto.getWorkspaceName(),
                fileModelDto.getMiniAppName(), fileModelDto.getFileName(),"bpmn");
        Uni<EventResponseModel> response = extensionService.fetchFileFromRepository(fileInfo);
        return response.onItem()
                .transform(
                        item ->
                        {
                            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                            documentBuilderFactory.setNamespaceAware(true);
                            Map<String, List<BPMNResourceModel>> dataList = new HashMap<String, List<BPMNResourceModel>>();
                            List<BPMNResourceModel> propertyList = new ArrayList<>();
                            String[] taskList = {"serviceTask", "scriptTask", "userTask", "businessRuleTask", "process"};
                            try {
                                File tempFile = FileOperations.createTempFileBind(item, "bpmn");
                                InputStream inputStream = new FileInputStream(tempFile);
                                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                                Document document = documentBuilder.parse(inputStream);
                                for(String taskName: taskList) {
                                    NodeList tasks = document.getElementsByTagNameNS("*" , taskName);
                                    List<BPMNResourceModel> list = new ArrayList<>();
                                    for(int index = 0; index < tasks.getLength(); index++) {
                                        Element task = (Element) tasks.item(index);
                                        BPMNResourceModel model = new BPMNResourceModel();
                                        model.setId(task.getAttribute("id"));
                                        model.setName(task.getAttribute("name"));
                                        list.add(model);
                                    }
                                    dataList.put(taskName, list);
                                }
                                NodeList properties = document.getElementsByTagNameNS("*" , "property");
                                NodeList itemDefs = document.getElementsByTagNameNS("*" , "itemDefinition");
                                for(int index = 0; index < properties.getLength(); index++) {
                                    Element property = (Element) properties.item(index);
                                    for(int i = 0; i< itemDefs.getLength(); i++) {
                                        Element itemDef = (Element) itemDefs.item(i);
                                        if(property.getAttribute("itemSubjectRef").equals(itemDef.getAttribute("id"))) {
                                            BPMNResourceModel model = new BPMNResourceModel();
                                            model.setId(property.getAttribute("id"));
                                            model.setName(property.getAttribute("name"));
                                            model.setType(itemDef.getAttribute("structureRef"));
                                            propertyList.add(model);
                                        }
                                    }

                                }
                                dataList.put("processData", propertyList);
                                tempFile.delete();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (ParserConfigurationException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (SAXException e) {
                                e.printStackTrace();
                            }


                            System.out.println(item);
                            EventResponseModel dataModel = new EventResponseModel();
                            dataModel.setMessage("BPMN Data");
                            dataModel.setData(dataList);
                            return dataModel;
                        }
                );
    }

    @Override
    public Uni<EventResponseModel> createMultiple(BaseModelList baseModels, String flag, String userId) {
        return null;
    }

    @Override
    public Uni<EventResponseModel> bindMultiple(BindPropertyList propertyList) {
        //TODO: Repeated code of binding, could check to make generic
        FileInformation fileInfo = FileOperations.createFileInfoToFetch(propertyList.getPropertyList().get(0).getWorkspaceName(),
                propertyList.getPropertyList().get(0).getMiniAppName(), propertyList.getPropertyList().get(0).getBpmnFileName(),"bpmn");
        Uni<EventResponseModel> response = extensionService.fetchFileFromRepository(fileInfo);
        return response.onItem()
                .transform(
                        item -> {
                            EventResponseModel responseModel = new EventResponseModel();
                            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                            documentBuilderFactory.setNamespaceAware(true);
                            try {
                                File tempFile = File.createTempFile("temp-",".bpmn");
                                Files.write(tempFile.toPath(), Collections.singleton(item.getData().toString()));
                                InputStream inputStream = new FileInputStream(tempFile);

                                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

                                Document document = documentBuilder.parse(inputStream);

                                NodeList definitions = document.getElementsByTagNameNS("*" , "definitions");
                                NodeList processes = document.getElementsByTagNameNS("*" , "process");
                                NodeList processChildren = processes.item(0).getChildNodes();
                                boolean variableExists = false;

                                for(BasePropertyDto basePropertyDto : propertyList.getPropertyList()) {

                                    for( int index = 0 ; index < processChildren.getLength() ; index++){
                                        //Verifying if data model with same name exists
                                        Node data = processChildren.item(index);

                                        if(Objects.nonNull(data.getAttributes()) &&
                                                Objects.nonNull(data.getAttributes().getNamedItem("name")) &&
                                                data.getAttributes().getNamedItem("name").getNodeValue().equals(basePropertyDto.getPropertyName())){
                                            variableExists = true;
                                        }
                                    }

                                    if(!variableExists) {
                                        Node process = processes.item(0);
                                        Element property = document.createElement( "bpmn2:property");

                                        property.setAttribute("id", basePropertyDto.getPropertyName());
                                        property.setAttribute("name", basePropertyDto.getPropertyName());
                                        property.setAttribute("itemSubjectRef", basePropertyDto.getId());


                                        process.appendChild(property);

                                        Node definition = definitions.item(0);

                                        Element itemDefinition = document.createElement( "bpmn2:itemDefinition");

                                        itemDefinition.setAttribute("id", basePropertyDto.getId());
                                        itemDefinition.setAttribute("structureRef", basePropertyDto.getQualifiedObjectName());


                                        definition.appendChild(itemDefinition);

                                    } else {
                                        responseModel.setMessage("Object for Model with same name already exists");
                                    }

                                }

                                if(!variableExists) {
                                    this.writeXMLFile(document , tempFile);
                                    fileInfo.setContent(Files.readString(tempFile.toPath()));
                                    fileInfo.setComment("Model appended to BPMN");
                                    extensionService.updateFileInRepository(fileInfo).subscribe().with(
                                            item1 -> System.out.println("Binded model to BPMN")
                                    );
                                    responseModel.setMessage("The binding was successfull");
                                }

                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (ParserConfigurationException e) {
                                e.printStackTrace();
                            } catch (SAXException e) {
                                e.printStackTrace();
                            } catch (TransformerException e) {
                                e.printStackTrace();
                            }

                            return responseModel;
                        }
                );
    }

    //Method to write document to the XML BPMN file
    private void writeXMLFile(Document doc, File bpmnFile)
            throws TransformerFactoryConfigurationError, TransformerException {
        doc.getDocumentElement().normalize();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer( );
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(bpmnFile);

        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

        transformer.transform(source, result);
        System.out.println("XML file updated successfully");
    }

}
