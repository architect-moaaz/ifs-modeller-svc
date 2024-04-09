package io.intelliflow.service;

import io.intelliflow.centralCustomExceptionHandler.CustomException;
import io.intelliflow.dto.model.BaseDataModelDTO;
import io.intelliflow.dto.model.DataModelDto;
import io.intelliflow.dto.model.DataModelProperty;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.dto.repomanager.FileInformation;
import io.quarkus.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.validation.constraints.NotBlank;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

public class  FileOperations {

    public static File createFile(String fileName, String fileExtension) {
        File newFile = null;
        try{
            if(fileName != null && fileExtension != null){
                switch (fileExtension){
                    case "txt":
                        newFile = File.createTempFile(fileName ,".txt");
                        break;
                    case "bpmn":
                        newFile = File.createTempFile(fileName ,".bpmn");
                        break;
                    case "doc":
                        newFile = File.createTempFile(fileName , ".doc");
                        break;
                }
            }
        } catch (IOException e){
            System.out.println("Exception occurred During file creation in FileOps!!");
            e.printStackTrace();
        }
        return newFile;
    }

    public static void writeToFile(File file, String data){
        if(file.isFile() && data != null){
            try{
                FileWriter writer = new FileWriter(file);
                PrintWriter pw =new PrintWriter(writer);
                pw.print(data);
                pw.close();
            } catch (IOException e){
                System.out.println("An error Occurred During File writing \n Stacktrace:");
                e.printStackTrace();
            }
        }
    }

    public static void deleteFile(File file){
        if(file.exists()){
            file.delete();
        }
    }


    private static JSONArray createJson(List<DataModelProperty> dataList) {
        JSONArray dataArray = new JSONArray();
        Set<String> addedNames = new HashSet<>();

        if (dataList != null) {
            for (DataModelProperty prop : dataList) {
                JSONObject obj = new JSONObject();
                try {
                    String propName = Character.toLowerCase(prop.getName().charAt(0)) + prop.getName().substring(1);
                    if (addedNames.contains(propName)) {
                        return null;
                    }
                    addedNames.add(propName);

                    obj.put("name", propName);
                    obj.put("mandatory", prop.getMandatory());

                    if (!prop.getPrimitive() || Objects.nonNull(prop.getCollectionType())) {
                        if (Objects.nonNull(prop.getCollectionType())) {
                            // If it's a collection
                            if (Objects.nonNull(prop.getValueType())) {
                                // If it's a Map
                                obj.put("type", prop.getValueType());
                                obj.put("isCollection", true);
                                if (prop.getValueTypePrimitive()) {
                                    // If it's Map with a primitive value
                                    obj.put("isPrimitive", true);
                                } else {
                                    // If it's Map with a non-primitive value
                                    obj.put("isPrimitive", false);
                                }
                            } else {
                                // If it's a list
                                obj.put("type", prop.getType());
                                obj.put("isCollection", true);
                                if (prop.getPrimitive()) {
                                    // List of Primitive
                                    obj.put("isPrimitive", true);
                                } else {
                                    // List of non-Primitive
                                    obj.put("isPrimitive", false);
                                }
                            }
                        } else {
                            obj.put("type", prop.getType());
                            obj.put("isCollection", false);
                            obj.put("isPrimitive", false);
                        }
                    } else {
                        obj.put("type", prop.getType());
                        obj.put("isCollection", false);
                        obj.put("isPrimitive", true);
                    }
                    dataArray.put(obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return dataArray;
    }

    /*
          Method to delete files with directories
       */
    public static void deleteFiles(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Log.error("Temporary File/Folder Deletion Failed!!");
            e.printStackTrace();
        }
    }

    public static FileInformation createFileInfoForMeta(BaseDataModelDTO baseDataModelDTO) throws CustomException {
        FileInformation metaFile = new FileInformation();
        metaFile.setWorkspaceName(baseDataModelDTO.getWorkspaceName());
        metaFile.setMiniApp(baseDataModelDTO.getMiniAppName());
        metaFile.setFileName(baseDataModelDTO.getFileName());
        if(baseDataModelDTO.getFileType().equalsIgnoreCase("datamodel")) {
            metaFile.setFileName(baseDataModelDTO.getFileName().substring(0,1).toUpperCase() + baseDataModelDTO.getFileName().substring(1));
            DataModelDto  dataModelDTO = (DataModelDto) baseDataModelDTO;
            //Adding _id for datamodel if not present
            //TODO:Need to optimize
            if(Objects.nonNull(dataModelDTO.getDataModelProperties()) &&
                    !checkForId(dataModelDTO.getDataModelProperties())) {
                DataModelProperty prop = new DataModelProperty();
                prop.setName("_id");
                prop.setType("String");
                List<DataModelProperty> propsList = dataModelDTO.getDataModelProperties();
                propsList.add(prop);
                dataModelDTO.setDataModelProperties(propsList);
            }
        JSONArray datamodeljson = createJson(dataModelDTO.getDataModelProperties());
            if (datamodeljson == null) {
                    throw new CustomException("Duplicate datatype name found");
            }
            metaFile.setContent(datamodeljson.toString());

            metaFile.setFileType("datamodel");
            } else if(baseDataModelDTO.getFileType().equalsIgnoreCase("drl")) {
                metaFile.setFileType("dmn");
                if(baseDataModelDTO.getFileContent() != null) {
                    metaFile.setContent(new String(baseDataModelDTO.getFileContent()));
                }
            } else if(baseDataModelDTO.getFileType().equalsIgnoreCase("bpmn")) {
                metaFile.setFileType("bpmn");
                if(baseDataModelDTO.getFileContent() != null) {
                    try {
                        File tempFile = File.createTempFile("temp-",".bpmn");
                        Files.write(tempFile.toPath(), Collections.singleton(new String(baseDataModelDTO.getFileContent())));
                        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

                        documentBuilderFactory.setNamespaceAware(true);
                        InputStream inputStream = new FileInputStream(tempFile);
                        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                        Document document = documentBuilder.parse(inputStream);
                        JSONArray propertyList = new JSONArray();

                        NodeList properties = document.getElementsByTagNameNS("*" , "property");
                        NodeList itemDefs = document.getElementsByTagNameNS("*" , "itemDefinition");
                        for(int index = 0; index < properties.getLength(); index++) {
                            Element property = (Element) properties.item(index);
                            for(int i = 0; i< itemDefs.getLength(); i++) {
                                Element itemDef = (Element) itemDefs.item(i);
                                if(property.getAttribute("itemSubjectRef").equals(itemDef.getAttribute("id"))) {
                                    JSONObject dataObj = new JSONObject();
                                    dataObj.put("id", property.getAttribute("id"));
                                    dataObj.put("name", property.getAttribute("name"));
                                    dataObj.put("namespace", itemDef.getAttribute("structureRef"));
                                    propertyList.put(dataObj);
                                }
                            }

                        }
                        metaFile.setContent(String.valueOf(propertyList));

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    } catch (SAXException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            } else {
                metaFile.setFileType(baseDataModelDTO.getFileType());
                if(baseDataModelDTO.getFileContent() != null) {
                    metaFile.setContent(new String(baseDataModelDTO.getFileContent()));
                }
            }
        metaFile.setComment("Created meta file for " + baseDataModelDTO.getFileName());
        return metaFile;
    }

    public static FileInformation createFileInfoToFetch(
            @NotBlank String workspaceName,
            @NotBlank String miniAppName,
            @NotBlank String fileName,
            @NotBlank String fileType) {

        return new FileInformation(
                fileName,
                fileType,
                workspaceName,
                miniAppName,
                null,
                null,
                null,
                null
        );
    }

    public static File createTempFileBind(EventResponseModel item, String fileType) {
       try {
           File tempFile = File.createTempFile("temp-","." + fileType);
           Files.write(tempFile.toPath(), Collections.singleton(item.getData().toString()));
           return tempFile;
       } catch (IOException e) {
           e.printStackTrace();
       }
        return null;
    }

    private static boolean checkForId(List<DataModelProperty> properties) {
        for(DataModelProperty prop : properties) {
            if(prop.getName().equals("_id")) {
                return true;
            }
        }
        return false;
    }
}
