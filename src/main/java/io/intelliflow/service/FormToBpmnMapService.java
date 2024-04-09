package io.intelliflow.service;

import com.jayway.jsonpath.JsonPath;
import io.intelliflow.dto.model.PageModel;
import io.intelliflow.dto.model.TaskModel;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.dto.repomanager.FileInformation;
import io.intelliflow.service.repomanager.ExtensionService;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

/*
    Class created to map and Form to BPMN
    TODO: Could make the code modular, see considerations
 */
public class FormToBpmnMapService {


    public Uni<EventResponseModel> mapRoleToPage(PageModel pageModel, ExtensionService extensionService) {
        FileInformation widFileInfo = FileOperations.createFileInfoToFetch(
                pageModel.getWorkspaceName(),
                pageModel.getMiniAppName(),
                null,
                "workflow"
        );
        Uni<EventResponseModel> widResponse = extensionService.fetchFileFromRepository(widFileInfo);
        EventResponseModel responseModel = new EventResponseModel();
        return widResponse.onItem()
                .transformToUni(
                        item -> {
                            try {
                                File tempWidFile = FileOperations.createTempFileBind(item, "wid");
                                JSONArray roleMappingArray = new JSONArray();
                                String jsonPathExpression;
                                if(Objects.nonNull(pageModel.getRoleId()) && pageModel.getRoleId() != "") {
                                    responseModel.setMessage("Role ID Updated");
                                    jsonPathExpression ="$.configuration.rolemapping[?(@.roleid=='" + pageModel.getRoleId() + "')]";
                                    net.minidev.json.JSONArray jsonNode = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(jsonPathExpression);

                                    String getRoleMapExpression = "$.configuration.rolemapping";
                                    net.minidev.json.JSONArray existingRoleMap = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(getRoleMapExpression);

                                    //Backing up existing mapping config
                                    String getMappingExpression = "$.configuration.mapping";
                                    net.minidev.json.JSONArray exisitingMapArray = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(getMappingExpression);

                                    if(jsonNode.size() == 0) {
                                        //There is no mapping for role id existing
                                        for(int i = 0; i < existingRoleMap.size(); i++){
                                            roleMappingArray.put(existingRoleMap.get(i));
                                        }
                                    } else {
                                        //Remove the existing mapping if exists
                                        for(int i = 0; i < existingRoleMap.size(); i++) {
                                            if(existingRoleMap.get(i).getClass().equals(LinkedHashMap.class)) {
                                                if(!((LinkedHashMap)existingRoleMap.get(i)).get("roleid").equals(pageModel.getRoleId())) {
                                                    roleMappingArray.put(existingRoleMap.get(i));
                                                }
                                            }
                                        }
                                    }

                                    JSONObject mappingObject = new JSONObject();
                                    mappingObject.put("roleid", pageModel.getRoleId());
                                    mappingObject.put("page", pageModel.getFileName());

                                    roleMappingArray.put(mappingObject);

                                    JSONObject widObject = new JSONObject(Files.readString(tempWidFile.toPath()));
                                    JSONObject newConfig = new JSONObject();
                                    newConfig.put("mapping", exisitingMapArray);
                                    newConfig.put("rolemapping", roleMappingArray);
                                    widObject.remove("configuration");
                                    widObject.put("configuration", newConfig);

                                    widFileInfo.setContent(widObject.toString(2));

                                    widFileInfo.setComment("WID File created");
                                    extensionService.updateFileInRepository(widFileInfo).subscribe().with(
                                            saved -> System.out.println("WID FIle Saved")
                                    );

                                } else {
                                    responseModel.setMessage("No role id passed");
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }


                            return  Uni.createFrom().item(responseModel);
                        }
                );
    }

    public Uni<EventResponseModel> mapFormToBpmn(TaskModel taskModel, ExtensionService extensionService){
        FileInformation widFileInfo = FileOperations.createFileInfoToFetch(
                taskModel.getWorkspace(),
                taskModel.getMiniapp(),
                null,
                "workflow"
        );
        Uni<EventResponseModel> widResponse = extensionService.fetchFileFromRepository(widFileInfo);
        EventResponseModel responseModel = new EventResponseModel();
        return widResponse.onItem()
                .transformToUni(
                        item -> {
                            File tempWidFile = FileOperations.createTempFileBind(item, "wid");
                            JSONArray mappingArray = new JSONArray();
                            String jsonData = getJsonData(tempWidFile);
                            String jsonPathExpression;
                            if(taskModel.getTasktype().equalsIgnoreCase("PT")) {
                                //mapping process tasks
                                jsonPathExpression = "$.configuration.mapping[?(@.tasktype=='" + taskModel.getTasktype() + "'" +
                                        "&& @.bpmnname=='" + taskModel.getBpmnname() +"')]";
                            } else if(taskModel.getTasktype().equalsIgnoreCase("endtask")) {
                                //If its mapping end task
                                jsonPathExpression = "$.configuration.mapping[?(@.tasktype=='end'" +
                                        "&& @.bpmnname=='" + taskModel.getBpmnname() +"')]";
                            } else {
                                jsonPathExpression = "$.configuration.mapping[?(@.taskname=='" + taskModel.getTaskname() + "'" +
                                        "&& @.bpmnname=='" + taskModel.getBpmnname() +"')]";
                            }

                            net.minidev.json.JSONArray jsonNode = JsonPath.parse(jsonData).read(jsonPathExpression);
                            String getMappingExpression = "$.configuration.mapping";

                            net.minidev.json.JSONArray exisitingMapArray = JsonPath.parse(jsonData).read(getMappingExpression);
                            //If there is no existing mapping for the data passed
                            if(jsonNode.size() == 0){
                                for(int i = 0; i < exisitingMapArray.size(); i++){
                                    mappingArray.put(exisitingMapArray.get(i));
                                }
                                responseModel.setMessage("WID Updation Completed");
                            } else {
                                //Removes the existing PT task Mapping for update
                                if(taskModel.getTasktype().equalsIgnoreCase("PT")) {
                                    for(int i = 0; i < exisitingMapArray.size(); i++){
                                        if(exisitingMapArray.get(i).getClass().equals(LinkedHashMap.class)){
                                            if(((LinkedHashMap)exisitingMapArray.get(i)).get("tasktype").equals("PT")) {
                                                continue;
                                            } else {
                                                mappingArray.put(exisitingMapArray.get(i));
                                            }
                                        }
                                    }
                                } else if(taskModel.getTasktype().equalsIgnoreCase("endtask")) {
                                    //Remove existing end task for the bpmn
                                    for(int i = 0; i < exisitingMapArray.size(); i++){
                                        if(exisitingMapArray.get(i).getClass().equals(LinkedHashMap.class)){
                                            if(
                                                    (((LinkedHashMap)exisitingMapArray.get(i)).get("tasktype").equals("endTask")) &&
                                                            (((LinkedHashMap)exisitingMapArray.get(i)).get("bpmnname").equals(taskModel.getBpmnname()))) {
                                                continue;
                                            } else {
                                                mappingArray.put(exisitingMapArray.get(i));
                                            }
                                        }
                                    }
                                } else {
                                    //Removing existing UT for the passed taskname
                                    for(int i = 0; i < exisitingMapArray.size(); i++){
                                        if(exisitingMapArray.get(i).getClass().equals(LinkedHashMap.class)) {
                                            if(
                                                    (((LinkedHashMap)exisitingMapArray.get(i)).get("taskname").equals(taskModel.getTaskname())) &&
                                                            (((LinkedHashMap)exisitingMapArray.get(i)).get("bpmnname").equals(taskModel.getBpmnname()))
                                            ){
                                                continue;
                                            } else {
                                                mappingArray.put(exisitingMapArray.get(i));
                                            }
                                        }
                                    }
                                }
                                responseModel.setMessage("Mapping Updated for " + taskModel.getTaskname());
                            }
                            JSONObject mappingObject = new JSONObject();
                                try{
                                    mappingObject.put("formname", taskModel.getFormname());
                                    mappingObject.put("tasktype", taskModel.getTasktype());
                                    mappingObject.put("bpmnname", taskModel.getBpmnname());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                //Fetching the data models mapped on to the task
                                FileInformation fileInfo = FileOperations.createFileInfoToFetch(
                                        taskModel.getWorkspace(),
                                        taskModel.getMiniapp(),
                                        taskModel.getBpmnname(),
                                        "bpmn"
                                );
                                Uni<EventResponseModel> bpmnResponse = extensionService.fetchFileFromRepository(fileInfo);
                                return bpmnResponse.onItem()
                                        .transformToUni(
                                                bpmnItem -> {
                                                    File tempFile = FileOperations.createTempFileBind(bpmnItem, "bpmn");
                                                    int statusFlag = -1;
                                                    String[] varStringList = {"Integer", "String", "Float", "Object", "Boolean", "java.lang.String"};

                                                    if(taskModel.getTasktype().equalsIgnoreCase("PT") ||
                                                            taskModel.getTasktype().equalsIgnoreCase("endtask")) {
                                                        //Fetch Variables
                                                        NodeList itemDefenitions = CustomDomParser.getNodeList(tempFile, "itemDefinition", null);
                                                        JSONArray varArray = new JSONArray();
                                                        Map<String, String> varIdList = new HashMap<>();
                                                        for(int i =0; i< itemDefenitions.getLength(); i++) {
                                                            String varType = ((Element) itemDefenitions.item(i)).getAttribute("structureRef");
                                                            if(!Stream.of(varStringList).anyMatch(varType::equalsIgnoreCase)){
                                                                varIdList.put(
                                                                        ((Element) itemDefenitions.item(i)).getAttribute("id"),
                                                                        varType);
                                                            }
                                                        }
                                                        NodeList processes = CustomDomParser.getNodeList(tempFile, "process", null);
                                                        NodeList processChild = processes.item(0).getChildNodes();
                                                        for(int i = 0; i< processChild.getLength(); i++) {
                                                            if(Objects.nonNull(processChild.item(i).getLocalName()) &&
                                                                    processChild.item(i).getLocalName().equals("property")) {
                                                                for(var varRef : varIdList.entrySet()){
                                                                    if((varRef.getKey()).equalsIgnoreCase(
                                                                            ((Element) processChild.item(i)).getAttribute("itemSubjectRef")
                                                                    )){
                                                                        JSONObject varObj = new JSONObject();
                                                                        try{
                                                                            varObj.put(
                                                                                    ((Element) processChild.item(i)).getAttribute("name"),
                                                                                    varRef.getValue()
                                                                            );
                                                                            varArray.put(varObj);
                                                                        } catch (JSONException e) {
                                                                            e.printStackTrace();
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        Node process = processes.item(0);
                                                        statusFlag = 0;
                                                        try {
                                                            if(taskModel.getTasktype().equalsIgnoreCase("endtask")) {
                                                                mappingObject.put("taskname", "endTask");
                                                                mappingObject.put("taskid", "endTask");
                                                            } else {
                                                                mappingObject.put("taskname", process.getAttributes().getNamedItem("name").getNodeValue());
                                                                mappingObject.put("taskid", process.getAttributes().getNamedItem("id").getNodeValue());
                                                            }
                                                            if(varArray.length() > 0) {
                                                                mappingObject.put("datamodels", varArray);
                                                            }
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    } else {
                                                        String[] taskList = {"serviceTask", "scriptTask", "userTask", "businessRuleTask"};
                                                        for(String taskType : taskList){
                                                            NodeList tasks = CustomDomParser.getNodeList(tempFile, taskType, null);
                                                            for( int index = 0 ; index < tasks.getLength() ; index++){
                                                                Node task = tasks.item(index);
                                                                if(task.getAttributes().getNamedItem("name").getNodeValue().equalsIgnoreCase(taskModel.getTaskname())) {
                                                                    //Find variables
                                                                    JSONArray varArray = new JSONArray();
                                                                    NodeList chidTasks = task.getChildNodes();
                                                                    for(int j = 0; j < chidTasks.getLength(); j++) {
                                                                        if(Objects.nonNull(chidTasks.item(j).getLocalName()) &&
                                                                                chidTasks.item(j).getLocalName().equals("ioSpecification")) {
                                                                            NodeList ioSpecChild = chidTasks.item(j).getChildNodes();
                                                                            for(int k = 0; k < ioSpecChild.getLength(); k++) {
                                                                                if(Objects.nonNull(ioSpecChild.item(k).getLocalName()) &&
                                                                                        ioSpecChild.item(k).getLocalName().equals("dataInput")) {
                                                                                    String varType = ((Element) ioSpecChild.item(k)).getAttribute("drools:dtype");
                                                                                    if(!Stream.of(varStringList).anyMatch(varType::equalsIgnoreCase)) {
                                                                                        JSONObject varObj = new JSONObject();
                                                                                        try {
                                                                                            varObj.put(
                                                                                                    ((Element) ioSpecChild.item(k)).getAttribute("name"),
                                                                                                    varType
                                                                                            );
                                                                                            varArray.put(varObj);
                                                                                        } catch (JSONException e) {
                                                                                            e.printStackTrace();
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        } else if(taskType.equals("userTask") && Objects.nonNull(chidTasks.item(j).getLocalName()) &&
                                                                                chidTasks.item(j).getLocalName().equals("dataInputAssociation")) {
                                                                            //Picking out the Task Name in UserTask as taskid in workflow
                                                                            NodeList dataInputChild = chidTasks.item(j).getChildNodes();
                                                                            for(int k = 0; k < dataInputChild.getLength(); k++) {
                                                                                if(Objects.nonNull(dataInputChild.item(k).getLocalName()) &&
                                                                                        dataInputChild.item(k).getLocalName().equals("assignment")) {
                                                                                    NodeList assignmentList = dataInputChild.item(k).getChildNodes();
                                                                                    for(int q = 0; q < assignmentList.getLength(); q++) {
                                                                                        if(Objects.nonNull(assignmentList.item(q).getLocalName()) &&
                                                                                                assignmentList.item(q).getLocalName().equals("from") &&
                                                                                        !assignmentList.item(q).getTextContent().equals("true") &&
                                                                                                !assignmentList.item(q).getTextContent().equals("false")) {
                                                                                            try {
                                                                                                mappingObject.put("taskid",assignmentList.item(q).getTextContent());
                                                                                            } catch (JSONException e) {
                                                                                                throw new RuntimeException(e);
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }

                                                                        }
                                                                    }

                                                                    statusFlag = 0;
                                                                    try {
                                                                        mappingObject.put("taskname", task.getAttributes().getNamedItem("name").getNodeValue());
                                                                        //mappingObject.put("taskid", task.getAttributes().getNamedItem("name").getNodeValue());
                                                                        if(varArray.length() > 0) {
                                                                            mappingObject.put("datamodels", varArray);
                                                                        }
                                                                    } catch (JSONException e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                }
                                                            }

                                                        }
                                                    }
                                                    if(statusFlag == 0) {
                                                        mappingArray.put(mappingObject);
                                                        try{
                                                            JSONObject widObject = new JSONObject(jsonData);

                                                            //Replicating ecisting page mappings
                                                            String getRoleMapExpression = "$.configuration.rolemapping";
                                                            net.minidev.json.JSONArray existingRoleMap = JsonPath.parse(jsonData).read(getRoleMapExpression);

                                                            JSONObject newConfig = new JSONObject();
                                                            newConfig.put("mapping", mappingArray);
                                                            newConfig.put("rolemapping",existingRoleMap);
                                                            widObject.remove("configuration");
                                                            widObject.put("configuration", newConfig);
                                                            widFileInfo.setContent(widObject.toString(2));
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                        widFileInfo.setComment("WID File created");
                                                        extensionService.updateFileInRepository(widFileInfo).subscribe().with(
                                                                saved -> System.out.println("WID FIle Saved")
                                                        );
                                                    } else {
                                                        responseModel.setMessage("Mentioned taskname not found");
                                                    }
                                                    tempFile.delete();
                                                    tempWidFile.delete();
                                                    return Uni.createFrom().item(responseModel);
                                                }
                                        );
                        }
                );
    }

    private String getJsonData(File tempWidFile) {
        try {
            return Files.readString(tempWidFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Uni<EventResponseModel> getMappings(String workspaceName, String appName,
                                               ExtensionService extensionService, boolean role) {
        FileInformation widFileInfo = FileOperations.createFileInfoToFetch(
                workspaceName,
                appName,
                null,
                "workflow"
        );
        Uni<EventResponseModel> widResponse = extensionService.fetchFileFromRepository(widFileInfo);
        EventResponseModel responseModel = new EventResponseModel();
        return widResponse.onItem()
                .transform(
                        item -> {
                            if(Objects.nonNull(item.getData())){
                                File tempWidFile = FileOperations.createTempFileBind(item, "wid");
                                String getMappingExpression = role ? "$.configuration.rolemapping" : "$.configuration.mapping";
                                try{
                                    net.minidev.json.JSONArray mapArray = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(getMappingExpression);
                                    responseModel.setData(mapArray);
                                    responseModel.setMessage("Exisiting Mapping for " + appName);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                tempWidFile.delete();
                            } else {
                                responseModel.setMessage("No Descriptor file Available for the App");
                            }
                            return responseModel;
                        }
                );
    }

    public Uni<EventResponseModel> removeMappings(TaskModel taskModel, ExtensionService extensionService) {
        FileInformation widFileInfo = FileOperations.createFileInfoToFetch(
                taskModel.getWorkspace(),
                taskModel.getMiniapp(),
                null,
                "workflow"
        );
        Uni<EventResponseModel> widResponse = extensionService.fetchFileFromRepository(widFileInfo);
        EventResponseModel responseModel = new EventResponseModel();
        return widResponse.onItem()
                .transform(
                        item -> {
                            if(Objects.nonNull(item.getData())){
                                File tempWidFile = FileOperations.createTempFileBind(item, "wid");
                                JSONArray mappingArray = new JSONArray();
                                String jsonPathExpression = "$.configuration.mapping[?(@.taskname=='" + taskModel.getTaskname() + "'" +
                                            "&& @.bpmnname=='" + taskModel.getBpmnname() +"')]";
                                try {
                                    net.minidev.json.JSONArray jsonNode = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(jsonPathExpression);
                                    if(jsonNode.size() == 0) {
                                        responseModel.setMessage("No Configuration found for specified data");
                                        return responseModel;
                                    } else {
                                        String getMappingExpression = "$.configuration.mapping";
                                        net.minidev.json.JSONArray exisitingMapArray = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(getMappingExpression);

                                        for(int i = 0; i < exisitingMapArray.size(); i++){
                                            if(exisitingMapArray.get(i).getClass().equals(LinkedHashMap.class)) {
                                                if(
                                                        (((LinkedHashMap)exisitingMapArray.get(i)).get("taskname").equals(taskModel.getTaskname())) &&
                                                                (((LinkedHashMap)exisitingMapArray.get(i)).get("bpmnname").equals(taskModel.getBpmnname()))
                                                ){
                                                    continue;
                                                } else {
                                                    mappingArray.put(exisitingMapArray.get(i));
                                                }
                                            }
                                        }
                                        JSONObject widObject = new JSONObject(Files.readString(tempWidFile.toPath()));
                                        JSONObject newConfig = new JSONObject();

                                        String getRoleMapExpression = "$.configuration.rolemapping";
                                        net.minidev.json.JSONArray existingRoleMap = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(getRoleMapExpression);

                                        newConfig.put("mapping", mappingArray);
                                        newConfig.put("rolemapping",existingRoleMap);
                                        widObject.remove("configuration");
                                        widObject.put("configuration", newConfig);
                                        widFileInfo.setContent(widObject.toString(2));
                                        widFileInfo.setComment("WID File created");
                                        extensionService.updateFileInRepository(widFileInfo).subscribe().with(
                                                saved -> System.out.println("WID FIle Updated")
                                        );
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                responseModel.setMessage("Configurations have been updated");
                                tempWidFile.delete();
                            } else {
                                responseModel.setMessage("No Descriptor file Available for the App");
                            }
                            return responseModel;
                        }
                );
    }

    public Uni<EventResponseModel> mapCreatedForms(List<TaskModel> taskModels, ExtensionService extensionService, String userId) {
        FileInformation widFileInfo = FileOperations.createFileInfoToFetch(
                taskModels.get(0).getWorkspace(),
                taskModels.get(0).getMiniapp(),
                null,
                "workflow"
        );
        Uni<EventResponseModel> widResponse = extensionService.fetchFileFromRepository(widFileInfo);
        return widResponse.onItem()
                .transform(
                        item -> {
                            File tempWidFile = FileOperations.createTempFileBind(item, "wid");
                            JSONArray mappingArray = new JSONArray();
                            try {
                                String getMappingExpression = "$.configuration.mapping";
                                net.minidev.json.JSONArray exisitingMapArray = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(getMappingExpression);

                                for(TaskModel taskModel : taskModels) {
                                    int flag = -1;
                                    for(int i = 0; i < exisitingMapArray.size(); i++){
                                        if(exisitingMapArray.get(i).getClass().equals(LinkedHashMap.class)) {
                                            if(
                                                    (((LinkedHashMap)exisitingMapArray.get(i)).get("taskname").equals(taskModel.getTaskname())) &&
                                                            (((LinkedHashMap)exisitingMapArray.get(i)).get("bpmnname").equals(taskModel.getBpmnname()))
                                            ){
                                                flag = 0;
                                            }
                                        }
                                    }
                                    if(flag == -1) {
                                        JSONObject mappingObject = new JSONObject();
                                        mappingObject.put("formname", taskModel.getFormname());
                                        mappingObject.put("tasktype", taskModel.getTasktype());
                                        mappingObject.put("bpmnname", taskModel.getBpmnname());
                                        mappingObject.put("taskname", taskModel.getTaskname());
                                        mappingObject.put("taskid", taskModel.getTaskid());
                                        mappingArray.put(mappingObject);
                                    }
                                }
                                for(int i = 0; i < exisitingMapArray.size(); i++){
                                    mappingArray.put(exisitingMapArray.get(i));
                                }
                                JSONObject widObject = new JSONObject(Files.readString(tempWidFile.toPath()));
                                JSONObject newConfig = new JSONObject();

                                String getRoleMapExpression = "$.configuration.rolemapping";
                                net.minidev.json.JSONArray existingRoleMap = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(getRoleMapExpression);
                                newConfig.put("rolemapping",existingRoleMap);
                                newConfig.put("mapping", mappingArray);
                                widObject.remove("configuration");
                                widObject.put("configuration", newConfig);
                                widFileInfo.setContent(widObject.toString(2));
                                widFileInfo.setComment("WID File created");
                                widFileInfo.setUserId(userId);
                                extensionService.updateFileInRepository(widFileInfo).subscribe().with(
                                        saved -> System.out.println("WID FIle Updated")
                                );
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            tempWidFile.delete();
                            return null;
                        }
                );
    }

    public Uni<EventResponseModel> removeRoleMapping(PageModel pageModel, ExtensionService extensionService) {

        FileInformation widFileInfo = FileOperations.createFileInfoToFetch(
                pageModel.getWorkspaceName(),
                pageModel.getMiniAppName(),
                null,
                "workflow"
        );
        Uni<EventResponseModel> widResponse = extensionService.fetchFileFromRepository(widFileInfo);
        EventResponseModel responseModel = new EventResponseModel();
        if(!Objects.nonNull(pageModel.getRoleId()) || pageModel.getRoleId() == "") {
            responseModel.setMessage("Role ID Invalid, please try again");
            return Uni.createFrom().item(() -> responseModel);
        }
        return widResponse.onItem()
                .transform(
                        item -> {
                            if(Objects.nonNull(item.getData())){

                                File tempWidFile = FileOperations.createTempFileBind(item, "wid");
                                JSONArray mappingArray = new JSONArray();
                                String jsonPathExpression = "$.configuration.rolemapping[?(@.roleid=='" + pageModel.getRoleId() + "')]";
                                try {
                                    net.minidev.json.JSONArray jsonNode = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(jsonPathExpression);
                                    if(jsonNode.size() == 0) {
                                        responseModel.setMessage("No Configuration found for specified data");
                                        return responseModel;
                                    } else {
                                        String getMappingExpression = "$.configuration.rolemapping";
                                        net.minidev.json.JSONArray exisitingMapArray = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(getMappingExpression);

                                        for(int i = 0; i < exisitingMapArray.size(); i++) {
                                            if(exisitingMapArray.get(i).getClass().equals(LinkedHashMap.class)) {
                                                if(
                                                        (((LinkedHashMap)exisitingMapArray.get(i)).get("roleid").equals(pageModel.getRoleId()))
                                                ){
                                                    continue;
                                                } else {
                                                    mappingArray.put(exisitingMapArray.get(i));
                                                }
                                            }
                                        }

                                        JSONObject widObject = new JSONObject(Files.readString(tempWidFile.toPath()));
                                        JSONObject newConfig = new JSONObject();

                                        String getRoleMapExpression = "$.configuration.mapping";
                                        net.minidev.json.JSONArray existingMap = JsonPath.parse(Files.readString(tempWidFile.toPath())).read(getRoleMapExpression);

                                        newConfig.put("rolemapping", mappingArray);
                                        newConfig.put("mapping",existingMap);
                                        widObject.remove("configuration");
                                        widObject.put("configuration", newConfig);
                                        widFileInfo.setContent(widObject.toString(2));
                                        widFileInfo.setComment("Role Mapping Updated");
                                        extensionService.updateFileInRepository(widFileInfo).subscribe().with(
                                                saved -> System.out.println("Role Mapping  Updated")
                                        );
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                responseModel.setMessage("Configurations have been updated");
                                tempWidFile.delete();
                            } else {
                                responseModel.setMessage("No Descriptor file Available for the App");
                            }
                            return responseModel;
                        }
                );
    }
}
