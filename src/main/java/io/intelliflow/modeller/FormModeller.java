package io.intelliflow.modeller;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import io.intelliflow.dto.bindproperty.BasePropertyDto;
import io.intelliflow.dto.bindproperty.BindPropertyList;
import io.intelliflow.dto.model.BaseDataModelDTO;
import io.intelliflow.dto.model.BaseModelList;
import io.intelliflow.dto.model.FileModelDto;
import io.intelliflow.dto.model.TaskModel;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.dto.repomanager.FileInformation;
import io.intelliflow.service.CustomDomParser;
import io.intelliflow.service.FileOperations;
import io.intelliflow.service.FormToBpmnMapService;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class FormModeller extends BaseModeller {

    @Override
    public void validate() {
    }

    @Override
    public Uni<EventResponseModel> generateFile(BaseDataModelDTO baseDataModelDTO, String flag, String userId) {
        if (baseDataModelDTO.getFileType().equalsIgnoreCase("bpmn")) {
            return generateFormFromBpmn(baseDataModelDTO, flag, userId);
        } else {
            return generateFormFromDataModel(baseDataModelDTO, flag, userId);
        }
    }

    @Override
    public Uni<EventResponseModel> bind(BasePropertyDto bpmnPropertyDto) {
        return null;
    }

    /*
     * Generate Form for the specified Data Model(Java Class)
     */
    private Uni<EventResponseModel> generateFormFromDataModel(BaseDataModelDTO baseDataModelDTO, String flag, String userId) {
        FileInformation fetchBpmnInfo = FileOperations.createFileInfoToFetch(
                baseDataModelDTO.getWorkspaceName(),
                baseDataModelDTO.getMiniAppName(),
                baseDataModelDTO.getFileName(),
                "datamodel");
        EventResponseModel responseModel = new EventResponseModel();

        Uni<EventResponseModel> response = extensionService.fetchFileFromRepository(fetchBpmnInfo);
        return response.onItem()
                .transform(
                        item -> {
                            File tempFile = FileOperations.createTempFileBind(item, "java");
                            try {
                                CompilationUnit cu = StaticJavaParser.parse(tempFile);
                                Map<String, String> variableProperties = new HashMap<>();
                                cu.findAll(VariableDeclarator.class).forEach(variable -> {
                                    if(!variable.toString().equalsIgnoreCase("serialVersionUID = 1L")){
                                        variableProperties.put(
                                                variable.toString(),
                                                String.valueOf(variable.getType())
                                        );
                                    }
                                });
                                FileInformation fileInformation = new FileInformation();
                                fileInformation.setWorkspaceName(baseDataModelDTO.getWorkspaceName());
                                fileInformation.setMiniApp(baseDataModelDTO.getMiniAppName());
                                fileInformation.setFileType("form");
                                String formName = Objects.nonNull(baseDataModelDTO.getUpdatedName()) ?
                                        baseDataModelDTO.getUpdatedName() :
                                        baseDataModelDTO.getFileName().substring(0,
                                        baseDataModelDTO.getFileName().lastIndexOf('.'));
                                fileInformation.setFileName(formName + ".frm");
                                System.out.println("Flag is " + flag);
                                    fileInformation.setContent(generateFormTemplateForExcel(
                                            variableProperties, baseDataModelDTO.getFileName(),
                                            flag,
                                            baseDataModelDTO.getFileName()));

                                fileInformation.setComment("Form created from " + baseDataModelDTO.getFileName());
                                fileInformation.setUserId(userId);
                                extensionService.createFileInRepo(fileInformation).subscribe()
                                        .with(
                                                item1 -> System.out.println("Created Form"));
                                responseModel.setMessage("Form " + formName + " has been created successfully");
                            } catch (IOException e) {
                                responseModel.setMessage("Error in writing file");
                                e.printStackTrace();
                            }
                            tempFile.delete();
                            return responseModel;
                        });
    }

    /*
     * Generate Forms from the given BPMN file
     */
    private Uni<EventResponseModel> generateFormFromBpmn(BaseDataModelDTO baseDataModelDTO, String flag, String userId) {
        FileInformation fetchBpmnInfo = FileOperations.createFileInfoToFetch(
                baseDataModelDTO.getWorkspaceName(),
                baseDataModelDTO.getMiniAppName(),
                baseDataModelDTO.getFileName(),
                "bpmn");
        EventResponseModel responseModel = new EventResponseModel();
        Uni<EventResponseModel> response = extensionService.fetchFileFromRepository(fetchBpmnInfo);
        return response.onItem()
                .transform(
                        item -> {
                            File tempFile = FileOperations.createTempFileBind(item, "bpmn");
                            List<TaskModel> taskModels = new ArrayList<>();
                            NodeList itemDefenitions = CustomDomParser.getNodeList(tempFile, "itemDefinition", null);
                            NodeList processes = CustomDomParser.getNodeList(tempFile, "process", null);
                            NodeList properties = CustomDomParser.getNodeList(tempFile, "property", null);
                            Map<String, String> processVariables = new HashMap<>();
                            for(int i =0; i< itemDefenitions.getLength(); i++) {
                                for(int j = 0; j < properties.getLength(); j++) {
                                    Element itemElement = (Element) itemDefenitions.item(i);
                                    Element propertyElement = (Element) properties.item(j);
                                    if(itemElement.getAttribute("id").equalsIgnoreCase(propertyElement.getAttribute("itemSubjectRef"))) {
                                        processVariables.put(
                                                propertyElement.getAttribute("id"),
                                                itemElement.getAttribute("structureRef")
                                        );
                                    }
                                }
                            }
                            generateFormContentPerFlag(processVariables, "form", null);
                            Node process = processes.item(0);

                            taskModels.add(createForms(
                                    process.getAttributes().getNamedItem("name").getNodeValue(),
                                    process.getAttributes().getNamedItem("id").getNodeValue(),
                                    processVariables,
                                    "process",
                                    baseDataModelDTO));
                            String[] taskList = {"serviceTask", "scriptTask", "userTask", "businessRuleTask"};
                            for(String taskType : taskList){
                                NodeList tasks = CustomDomParser.getNodeList(tempFile, taskType, null);
                                for (int index = 0; index < tasks.getLength(); index++) {
                                    Map<String, String> taskVariables = new HashMap<>();
                                    Node singleTask = tasks.item(index);
                                    Element userEle = (Element)tasks.item(index);
                                    NodeList inputList = userEle.getElementsByTagNameNS("*","dataInput");
                                    for(int j = 0; j < inputList.getLength(); j++) {
                                        Element inputElement = (Element) inputList.item(j);
                                        for(int k = 0; k < properties.getLength(); k++) {
                                            Element propertyElement = (Element) properties.item(k);
                                            if(inputElement.getAttribute("name").equalsIgnoreCase(propertyElement.getAttribute("id"))) {
                                                taskVariables.put(
                                                        inputElement.getAttribute("name"),
                                                        inputElement.getAttribute("drools:dtype")
                                                );
                                            }
                                        }
                                    }
                                    taskModels.add(createForms(
                                            singleTask.getAttributes().getNamedItem("name").getNodeValue(),
                                            singleTask.getAttributes().getNamedItem("name").getNodeValue(),
                                            taskVariables,
                                            taskType,
                                            baseDataModelDTO));
                                }

                            }
                            //Create the WID mapping for created forms
                            FormToBpmnMapService service = new FormToBpmnMapService();
                            service.mapCreatedForms(taskModels, extensionService, userId).subscribe().with(
                                    wid -> System.out.println("Wid Updated")
                            );
                            tempFile.delete();
                            responseModel.setMessage("Forms have been generated successfully from " + baseDataModelDTO.getFileName());
                            return responseModel;
                        });
    }

    /*
     * Creating forms from BPMN with nodename, type and basedata
     */
    private TaskModel createForms(String nodeName, String nodeId, Map<String, String> variables, String nodeType, BaseDataModelDTO baseDataModelDTO) {
        FileInformation formInfo = new FileInformation();
        TaskModel taskModel = new TaskModel();
        formInfo.setWorkspaceName(baseDataModelDTO.getWorkspaceName());
        formInfo.setMiniApp(baseDataModelDTO.getMiniAppName());
        formInfo.setFileType("form");
        if(nodeType.equals("process")) {
            taskModel.setTasktype("PT");
            taskModel.setFormname(nodeName + "_PT.frm");
            formInfo.setFileName(nodeName + "_PT.frm");
            formInfo.setContent(generateFormContentPerFlag(variables, "form", null));
        } else {
            taskModel.setTasktype("UT");
            taskModel.setFormname(nodeName + "_UT.frm");
            formInfo.setFileName(nodeName + "_UT.frm");
            formInfo.setContent(generateFormContentPerFlag(variables, "form", null));
        }
        formInfo.setComment("Created form for "+ nodeType + " named " + nodeName);
        taskModel.setWorkspace(baseDataModelDTO.getWorkspaceName());
        taskModel.setMiniapp(baseDataModelDTO.getMiniAppName());
        taskModel.setTaskname(nodeName);
        taskModel.setBpmnname(baseDataModelDTO.getFileName());
        taskModel.setTaskid(nodeId);
        extensionService.createFileInRepo(formInfo).subscribe()
                .with(
                        item -> System.out.println("Created Form"));
        return taskModel;
    }


    @Override
    public Uni<EventResponseModel> dataExtractor(FileModelDto fileModelDto) {
        return null;
    }

    @Override
    public Uni<EventResponseModel> createMultiple(BaseModelList baseModels, String flag, String userId) {
        EventResponseModel response = new EventResponseModel();
        for(BaseDataModelDTO datamodel : baseModels.getBaseModels()) {
            generateFormFromDataModel(datamodel, flag, userId).subscribe()
                    .with(
                            i -> System.out.println("Form File " + datamodel.getFileName() + " Created")
                    );
        }
        response.setMessage("Success");
        return Uni.createFrom().item(() -> response);
    }

    @Override
    public Uni<EventResponseModel> bindMultiple(BindPropertyList propertyList) {
        return null;
    }


    public String generateFormContentPerFlag(Map<String, String> variables, String flag, String fileName) {
        JSONArray variableArray = new JSONArray();
        int axis =0;
        if(!(Objects.nonNull(flag)) ||
                ("form".equalsIgnoreCase(flag) ||
                        "formGrid".equalsIgnoreCase(flag) ||
                        "formSheet".equalsIgnoreCase(flag) ||
                        "formGridSheet".equalsIgnoreCase(flag))){
            /*
                Generated Normal form content for form or
                formGrid where both form and grid are present
             */
            for(var variable : variables.entrySet()) {
                try {
                    int randNum = (int) Math.ceil(Math.random()*(999999999));
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", "0");
                    jsonObject.put("y", axis);
                    jsonObject.put("w", "4");
                    jsonObject.put("h", "1");
                    jsonObject.put("i", randNum);
                    jsonObject.put("id", randNum);
                    jsonObject.put("elementType", variable.getValue());
                    jsonObject.put("fieldType", variable.getKey());
                    jsonObject.put("placeholder", variable.getKey());
                    jsonObject.put("required", "");
                    jsonObject.put("edit", "");
                    jsonObject.put("multiSelect", "");
                    jsonObject.put("VAxis", "");
                    jsonObject.put("minChoices", "");
                    jsonObject.put("maxChoices", "");
                    jsonObject.put("choices", "");
                    jsonObject.put("fieldName", variable.getKey());
                    jsonObject.put("date", "");
                    jsonObject.put("accessibility", "");
                    jsonObject.put("prefix", "");
                    jsonObject.put("suffix", "");
                    jsonObject.put("dateFormat", "");
                    jsonObject.put("ratingType", "");
                    jsonObject.put("ratingScale", "");
                    jsonObject.put("rating", "");
                    jsonObject.put("fileType", variable.getKey());
                    jsonObject.put("minFilesLimit", "");
                    jsonObject.put("maxFilesLimit", "");
                    jsonObject.put("minFileSize", "");
                    jsonObject.put("maxFileSize", "");
                    jsonObject.put("processVariableName", variable.getKey());
                    jsonObject.put("minLength", "");
                    jsonObject.put("maxLength", "");
                    jsonObject.put("dateRangeStart", "");
                    jsonObject.put("dateRangeEnd", "");
                    jsonObject.put("processInputVariable", "");
                    jsonObject.put("dataGridProperties", "");
                    jsonObject.put("eSignatureProperties", "");
                    jsonObject.put("moved", "");
                    jsonObject.put("static", "");
                    jsonObject.put("toolTip", "");

                    variableArray.put(jsonObject);
                    axis+=1;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if("grid".equalsIgnoreCase(flag) || "formGrid".equalsIgnoreCase(flag)
            || "gridSheet".equalsIgnoreCase(flag) || "formGridSheet".equalsIgnoreCase(flag)){
             /*
                Generated Grid content for form or
                formGrid where both form and grid are present
             */
                try {

                    //Generate data grid properties
                    JSONArray gridColumns = new JSONArray();
                    for(var variable : variables.entrySet()) {
                        JSONObject columnObject = new JSONObject();
                        columnObject.put("name", variable.getKey());
                        columnObject.put("type", variable.getValue());
                        columnObject.put("required", true);
                        gridColumns.put(columnObject);
                    }

                    JSONObject gridProperties = new JSONObject();
                    gridProperties.put("dataModelName", fileName);
                    gridProperties.put("cols", gridColumns);
                    gridProperties.put("filters", new JSONArray());


                    int randNum = (int) Math.ceil(Math.random()*(999999999));
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", "0");
                    jsonObject.put("y", axis);
                    jsonObject.put("w", "4");
                    jsonObject.put("h", "1");
                    jsonObject.put("i", randNum);
                    jsonObject.put("id", randNum);
                    jsonObject.put("elementType", "dataGrid");
                    jsonObject.put("fieldType", "text");
                    jsonObject.put("placeholder", "Grid Table");
                    jsonObject.put("required", "");
                    jsonObject.put("edit", "");
                    jsonObject.put("multiSelect", "");
                    jsonObject.put("VAxis", "");
                    jsonObject.put("minChoices", "");
                    jsonObject.put("maxChoices", "");
                    jsonObject.put("choices", "");
                    jsonObject.put("fieldName", fileName);
                    jsonObject.put("date", "");
                    jsonObject.put("accessibility", "");
                    jsonObject.put("prefix", "");
                    jsonObject.put("suffix", "");
                    jsonObject.put("dateFormat", "");
                    jsonObject.put("ratingType", "");
                    jsonObject.put("ratingScale", "");
                    jsonObject.put("rating", "");
                    jsonObject.put("fileType", "");
                    jsonObject.put("minFilesLimit", "");
                    jsonObject.put("maxFilesLimit", "");
                    jsonObject.put("minFileSize", "");
                    jsonObject.put("maxFileSize", "");
                    jsonObject.put("processVariableName", fileName.substring(0, fileName.lastIndexOf(".")).toLowerCase());
                    jsonObject.put("minLength", "");
                    jsonObject.put("maxLength", "");
                    jsonObject.put("dateRangeStart", "");
                    jsonObject.put("dateRangeEnd", "");
                    jsonObject.put("processInputVariable", fileName.substring(0, fileName.lastIndexOf(".")).toLowerCase());
                    jsonObject.put("dataGridProperties", gridProperties);
                    jsonObject.put("eSignatureProperties", "");
                    jsonObject.put("moved", "");
                    jsonObject.put("static", "");
                    jsonObject.put("toolTip", "");

                    variableArray.put(jsonObject);
                    axis+=1;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }

        if("sheet".equalsIgnoreCase(flag) || "gridSheet".equalsIgnoreCase(flag) ||
                "formSheet".equalsIgnoreCase(flag) || "formGridSheet".equalsIgnoreCase(flag)) {
            System.out.println("Sheet from Other CAse");
            /*
                Generated Intellisheet content for form
             */
            try {
                int randNum = (int) Math.ceil(Math.random()*(999999999));
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("x", "0");
                jsonObject.put("y", axis);
                jsonObject.put("w", "10");
                jsonObject.put("h", "6");
                jsonObject.put("i", randNum);
                jsonObject.put("id", randNum);
                jsonObject.put("elementType", "intellisheet");
                jsonObject.put("fieldType", "text");
                jsonObject.put("placeholder", "");
                jsonObject.put("required", "");
                jsonObject.put("edit", false);
                jsonObject.put("multiSelect", "");
                jsonObject.put("VAxis", "");
                jsonObject.put("minChoices", "");
                jsonObject.put("maxChoices", "");
                jsonObject.put("choices", "");
                jsonObject.put("fieldName", "");
                jsonObject.put("date", "");
                jsonObject.put("accessibility", "");
                jsonObject.put("prefix", "");
                jsonObject.put("suffix", "");
                jsonObject.put("dateFormat", "");
                jsonObject.put("ratingType", "");
                jsonObject.put("ratingScale", "");
                jsonObject.put("rating", "");
                jsonObject.put("fileType", "");
                jsonObject.put("minFilesLimit", "");
                jsonObject.put("maxFilesLimit", "");
                jsonObject.put("minFileSize", "");
                jsonObject.put("maxFileSize", "");
                jsonObject.put("processVariableName", "");
                jsonObject.put("minLength", "");
                jsonObject.put("maxLength", "");
                jsonObject.put("dateRangeStart", "");
                jsonObject.put("dateRangeEnd", "");
                jsonObject.put("processInputVariable", "");
                jsonObject.put("dataGridProperties", "");
                jsonObject.put("eSignatureProperties", "");
                jsonObject.put("moved", "");
                jsonObject.put("static", "");
                jsonObject.put("toolTip", "");

                variableArray.put(jsonObject);
                axis+=1;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            JSONObject formObject = new JSONObject();
            formObject.put("formData", variableArray);
            formObject.put("claim", false);
            return formObject.toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String generateFormTemplateForExcel(Map<String, String> variables, String dataModelName, String flag, String fileName) {
        List<String> buttonList = Arrays.asList("Create", "Update", "Delete", "Prev", "Exit", "Next");

        System.out.println("Flag at Start " + flag);
        int xAxis = 1;
        int yAxis = 0;
        JSONArray variableArray = new JSONArray();
        if(Objects.nonNull(flag)) {
            //Invoked only when called from Excel to App
            String processVariableName = dataModelName.substring(0,1).toLowerCase() + dataModelName.substring(1);
            processVariableName = processVariableName.substring(0, processVariableName.indexOf("."));
            String modelName = dataModelName.substring(0, dataModelName.indexOf(".")+1);
            try {

                //For fields on form
                for(var variable : variables.entrySet()) {
                    if(Objects.nonNull(variable)) {
                        int randNum = (int) Math.ceil(Math.random()*(999999999));
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("x", xAxis);
                        jsonObject.put("y", yAxis);
                        jsonObject.put("w", 4);
                        jsonObject.put("h", 1);
                        jsonObject.put("i", randNum);
                        jsonObject.put("id", randNum);
                        jsonObject.put("elementType", getFormElementType(variable.getValue()));
                        jsonObject.put("fieldType", "text");
                        jsonObject.put("placeholder", "");
                        jsonObject.put("required", false);
                        jsonObject.put("edit", false);
                        jsonObject.put("multiSelect", "");
                        jsonObject.put("VAxis", "");
                        jsonObject.put("minChoices", "");
                        jsonObject.put("maxChoices", "");
                        jsonObject.put("choices", "");
                        jsonObject.put("fieldName", StringUtils.capitalize(variable.getKey()));
                        jsonObject.put("date", new Date().toString());
                        jsonObject.put("accessibility", "");
                        jsonObject.put("prefix", "");
                        jsonObject.put("suffix", "");
                        jsonObject.put("dateFormat", "");
                        jsonObject.put("ratingType", 5);
                        jsonObject.put("ratingScale", "");
                        jsonObject.put("rating", "");
                        jsonObject.put("fileType", "");
                        jsonObject.put("minFilesLimit", "");
                        jsonObject.put("maxFilesLimit", "");
                        jsonObject.put("minFileSize", "");
                        jsonObject.put("maxFileSize", "");
                        jsonObject.put("processVariableName", processVariableName + "." + variable.getKey());
                        jsonObject.put("minLength", "");
                        jsonObject.put("maxLength", "");
                        jsonObject.put("dateRangeStart", "");
                        jsonObject.put("dateRangeEnd", "");
                        jsonObject.put("eSignatureProperties", "");
                        jsonObject.put("isMathExpression", false);
                        jsonObject.put("actionType", "");
                        jsonObject.put("selectedDataModel", StringUtils.capitalize(modelName));
                        jsonObject.put("selectedDataField", variable.getKey());
                        jsonObject.put("moved", false);
                        jsonObject.put("static", false);
                        jsonObject.put("toolTip", "");

                        variableArray.put(jsonObject);
                        if(xAxis < 16) {
                            xAxis += 5;
                        } else {
                            xAxis = 1;
                            yAxis += 1;
                        }
                    }
                }

                xAxis = 3;
                yAxis += 2;
                //For Buttons in Form
                for(String buttonKey : buttonList) {
                    int randNum = (int) Math.ceil(Math.random()*(999999999));
                    JSONObject jsonObject = new JSONObject();
                    if (buttonKey.equals("Prev") || buttonKey.equals("Next")) {
                        jsonObject.put("bgColor", "");
                    }
                    jsonObject.put("x", xAxis);
                    jsonObject.put("y", yAxis);
                    jsonObject.put("w", 4);
                    jsonObject.put("h", "1");
                    jsonObject.put("i", randNum);
                    jsonObject.put("id", randNum);
                    jsonObject.put("elementType", "button");
                    jsonObject.put("fieldType", "text");
                    jsonObject.put("placeholder", "");
                    jsonObject.put("required", "");
                    jsonObject.put("edit", "");
                    jsonObject.put("multiSelect", "");
                    jsonObject.put("VAxis", "");
                    jsonObject.put("minChoices", "");
                    jsonObject.put("maxChoices", "");
                    jsonObject.put("choices", "");
                    jsonObject.put("fieldName", getFieldNameForButton(buttonKey));
                    jsonObject.put("date", new Date().toString());
                    jsonObject.put("accessibility", "");
                    jsonObject.put("prefix", "");
                    jsonObject.put("suffix", "");
                    jsonObject.put("dateFormat", "");
                    jsonObject.put("ratingType", 5);
                    jsonObject.put("ratingScale", "");
                    jsonObject.put("rating", "");
                    jsonObject.put("fileType", "");
                    jsonObject.put("minFilesLimit", "");
                    jsonObject.put("maxFilesLimit", "");
                    jsonObject.put("minFileSize", "");
                    jsonObject.put("maxFileSize", "");
                    jsonObject.put("processVariableName", "action");
                    jsonObject.put("minLength", "");
                    jsonObject.put("maxLength", "");
                    jsonObject.put("dateRangeStart", "");
                    jsonObject.put("dateRangeEnd", "");
                    jsonObject.put("eSignatureProperties", "");
                    jsonObject.put("isMathExpression", false);
                    jsonObject.put("actionType", buttonKey.toUpperCase());
                    jsonObject.put("selectedDataModel", "");
                    jsonObject.put("selectedDataField", "");
                    jsonObject.put("moved", "");
                    jsonObject.put("static", "");
                    jsonObject.put("toolTip", "");

                    variableArray.put(jsonObject);
                    if(xAxis < 13) {
                        xAxis += 5;
                    } else {
                        xAxis = 3;
                        yAxis += 1;
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }



        /*
            Generating sheets, Grid or form elements according to flags
         */
        xAxis = 1;
        yAxis += 1;
        if(!(Objects.nonNull(flag)) ||
                ("form".equalsIgnoreCase(flag) ||
                        "formGrid".equalsIgnoreCase(flag) ||
                        "formSheet".equalsIgnoreCase(flag) ||
                        "formGridSheet".equalsIgnoreCase(flag))){
            /*
                Generated Normal form content for form or
                formGrid where both form and grid are present
             */
            for(var variable : variables.entrySet()) {
                try {
                    int randNum = (int) Math.ceil(Math.random()*(999999999));
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", xAxis);
                    jsonObject.put("y", yAxis);
                    jsonObject.put("w", "4");
                    jsonObject.put("h", "1");
                    jsonObject.put("i", randNum);
                    jsonObject.put("id", randNum);
                    jsonObject.put("elementType", variable.getValue());
                    jsonObject.put("fieldType", variable.getKey());
                    jsonObject.put("placeholder", variable.getKey());
                    jsonObject.put("required", "");
                    jsonObject.put("edit", "");
                    jsonObject.put("multiSelect", "");
                    jsonObject.put("VAxis", "");
                    jsonObject.put("minChoices", "");
                    jsonObject.put("maxChoices", "");
                    jsonObject.put("choices", "");
                    jsonObject.put("fieldName", variable.getKey());
                    jsonObject.put("date", "");
                    jsonObject.put("accessibility", "");
                    jsonObject.put("prefix", "");
                    jsonObject.put("suffix", "");
                    jsonObject.put("dateFormat", "");
                    jsonObject.put("ratingType", "");
                    jsonObject.put("ratingScale", "");
                    jsonObject.put("rating", "");
                    jsonObject.put("fileType", variable.getKey());
                    jsonObject.put("minFilesLimit", "");
                    jsonObject.put("maxFilesLimit", "");
                    jsonObject.put("minFileSize", "");
                    jsonObject.put("maxFileSize", "");
                    jsonObject.put("processVariableName", variable.getKey());
                    jsonObject.put("minLength", "");
                    jsonObject.put("maxLength", "");
                    jsonObject.put("dateRangeStart", "");
                    jsonObject.put("dateRangeEnd", "");
                    jsonObject.put("processInputVariable", "");
                    jsonObject.put("dataGridProperties", "");
                    jsonObject.put("eSignatureProperties", "");
                    jsonObject.put("moved", "");
                    jsonObject.put("static", "");
                    jsonObject.put("toolTip", "");

                    variableArray.put(jsonObject);
                    yAxis+=1;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if("grid".equalsIgnoreCase(flag) || "formGrid".equalsIgnoreCase(flag)
                || "gridSheet".equalsIgnoreCase(flag) || "formGridSheet".equalsIgnoreCase(flag)){
             /*
                Generated Grid content for form or
                formGrid where both form and grid are present
             */
            try {

                //Generate data grid properties
                JSONArray gridColumns = new JSONArray();
                for(var variable : variables.entrySet()) {
                    JSONObject columnObject = new JSONObject();
                    columnObject.put("name", variable.getKey());
                    columnObject.put("type", variable.getValue());
                    columnObject.put("required", true);
                    gridColumns.put(columnObject);
                }

                JSONObject gridProperties = new JSONObject();
                gridProperties.put("dataModelName", fileName);
                gridProperties.put("cols", gridColumns);
                gridProperties.put("filters", new JSONArray());


                int randNum = (int) Math.ceil(Math.random()*(999999999));
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("x", xAxis);
                jsonObject.put("y", yAxis);
                jsonObject.put("w", "4");
                jsonObject.put("h", "1");
                jsonObject.put("i", randNum);
                jsonObject.put("id", randNum);
                jsonObject.put("elementType", "dataGrid");
                jsonObject.put("fieldType", "text");
                jsonObject.put("placeholder", "Grid Table");
                jsonObject.put("required", "");
                jsonObject.put("edit", "");
                jsonObject.put("multiSelect", "");
                jsonObject.put("VAxis", "");
                jsonObject.put("minChoices", "");
                jsonObject.put("maxChoices", "");
                jsonObject.put("choices", "");
                jsonObject.put("fieldName", fileName);
                jsonObject.put("date", "");
                jsonObject.put("accessibility", "");
                jsonObject.put("prefix", "");
                jsonObject.put("suffix", "");
                jsonObject.put("dateFormat", "");
                jsonObject.put("ratingType", "");
                jsonObject.put("ratingScale", "");
                jsonObject.put("rating", "");
                jsonObject.put("fileType", "");
                jsonObject.put("minFilesLimit", "");
                jsonObject.put("maxFilesLimit", "");
                jsonObject.put("minFileSize", "");
                jsonObject.put("maxFileSize", "");
                jsonObject.put("processVariableName", fileName.substring(0, fileName.lastIndexOf(".")).toLowerCase());
                jsonObject.put("minLength", "");
                jsonObject.put("maxLength", "");
                jsonObject.put("dateRangeStart", "");
                jsonObject.put("dateRangeEnd", "");
                jsonObject.put("processInputVariable", fileName.substring(0, fileName.lastIndexOf(".")).toLowerCase());
                jsonObject.put("dataGridProperties", gridProperties);
                jsonObject.put("eSignatureProperties", "");
                jsonObject.put("moved", "");
                jsonObject.put("static", "");
                jsonObject.put("toolTip", "");

                variableArray.put(jsonObject);
                yAxis+=1;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if("sheet".equalsIgnoreCase(flag) || "gridSheet".equalsIgnoreCase(flag) ||
                "formSheet".equalsIgnoreCase(flag) || "formGridSheet".equalsIgnoreCase(flag)) {
            System.out.println("Sheet from Excel CAse with Falg " + flag);
            /*
                Generated Intellisheet content for form
             */
            try {
                int randNum = (int) Math.ceil(Math.random()*(999999999));
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("x", xAxis);
                jsonObject.put("y", yAxis);
                jsonObject.put("w", "10");
                jsonObject.put("h", "6");
                jsonObject.put("i", randNum);
                jsonObject.put("id", randNum);
                jsonObject.put("elementType", "intellisheet");
                jsonObject.put("fieldType", "text");
                jsonObject.put("placeholder", "");
                jsonObject.put("required", "");
                jsonObject.put("edit", false);
                jsonObject.put("multiSelect", "");
                jsonObject.put("VAxis", "");
                jsonObject.put("minChoices", "");
                jsonObject.put("maxChoices", "");
                jsonObject.put("choices", "");
                jsonObject.put("fieldName", "");
                jsonObject.put("date", "");
                jsonObject.put("accessibility", "");
                jsonObject.put("prefix", "");
                jsonObject.put("suffix", "");
                jsonObject.put("dateFormat", "");
                jsonObject.put("ratingType", "");
                jsonObject.put("ratingScale", "");
                jsonObject.put("rating", "");
                jsonObject.put("fileType", "");
                jsonObject.put("minFilesLimit", "");
                jsonObject.put("maxFilesLimit", "");
                jsonObject.put("minFileSize", "");
                jsonObject.put("maxFileSize", "");
                jsonObject.put("processVariableName", "");
                jsonObject.put("minLength", "");
                jsonObject.put("maxLength", "");
                jsonObject.put("dateRangeStart", "");
                jsonObject.put("dateRangeEnd", "");
                jsonObject.put("processInputVariable", "");
                jsonObject.put("dataGridProperties", "");
                jsonObject.put("eSignatureProperties", "");
                jsonObject.put("moved", "");
                jsonObject.put("static", "");
                jsonObject.put("toolTip", "");

                variableArray.put(jsonObject);
                yAxis+=1;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            JSONObject formObject = new JSONObject();
            formObject.put("formData", variableArray);
            formObject.put("claim", false);
            return formObject.toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }


    private String getFormElementType(String dataType) {
        switch (dataType) {
            case "Integer":
                return "number";
            default:
                return "text";
        }
    }

    private String getFieldNameForButton(String key) {
        if(key.equals("Prev")) {
            return "<div style=\"text-align: center;\"><b style=\"color: var(--bs-modal-color); text-align: var(--bs-body-text-align);\">&lt;&lt; Prev</b></div>";
        } else if (key.equals("Next")) {
            return "<div style=\"text-align: center;\"><b style=\"color: var(--bs-modal-color); text-align: var(--bs-body-text-align);\">Next &gt;&gt;</b></div>";
        } else {
            return key;
        }
    }
}
