package io.intelliflow.modeller;

import com.helger.jcodemodel.*;
import com.helger.jcodemodel.writer.JCMWriter;
import io.intelliflow.centralCustomExceptionHandler.CustomException;
import io.intelliflow.dto.bindproperty.BasePropertyDto;
import io.intelliflow.dto.bindproperty.BindPropertyList;
import io.intelliflow.dto.model.*;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.dto.repomanager.FileInformation;
import io.intelliflow.service.ClassProperties;
import io.intelliflow.service.FileOperations;
import io.intelliflow.utils.CreateClassOnTheGo;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Singleton
public class DataModeller extends BaseModeller {

    @ConfigProperty(name = "code.generated.path")
    String packageName;

    @Inject
    CreateClassOnTheGo createClassOnTheGo;

    @Override
    public void validate() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Uni<EventResponseModel> generateFile(BaseDataModelDTO baseDataModelDTO, String flag, String userId) {

        FileInformation fetchFileInfo = FileOperations.createFileInfoToFetch(
                baseDataModelDTO.getWorkspaceName(), baseDataModelDTO.getMiniAppName(), baseDataModelDTO.getFileName(), "form"
        );
        Uni<EventResponseModel> response = extensionService.fetchFileFromRepository(fetchFileInfo);
        return response.onItem()
                .transform(
                        item -> {
                            File tempFile = FileOperations.createTempFileBind(item, "frm");
                            DataModelDto newDataModel = new DataModelDto();
                            newDataModel.setWorkspaceName(baseDataModelDTO.getWorkspaceName());
                            newDataModel.setMiniAppName(baseDataModelDTO.getMiniAppName());
                            newDataModel.setFileName(baseDataModelDTO.getFileName().substring(
                                    0, baseDataModelDTO.getFileName().lastIndexOf('.')));
                            newDataModel.setFileType("datamodel");
                            //TODO: Change the file, currently taking a local file
                            newDataModel = generateModelFromForm(tempFile, newDataModel);
                            try {
                                createFile(newDataModel, userId).subscribe().with(
                                        item1 -> {
                                            System.out.println(item1);
                                        }
                                );
                            } catch (CustomException e) {
                                throw new RuntimeException(e);
                            }
                            EventResponseModel responseModel = new EventResponseModel();
                            responseModel.setMessage("Data Model has been generated Successfully");
                            return responseModel;
                        }
                );
    }

    public Uni<EventResponseModel> createFile(BaseDataModelDTO baseModelDto, String userId) throws CustomException {
        DataModelDto dataModelDto = (DataModelDto) baseModelDto;
        String modelName = StringUtils.capitalize(dataModelDto.getFileName().replaceAll("\\s", ""));
        dataModelDto.setFileName(modelName);
        baseModelDto.setFileName(modelName);
        FileInformation metaFile = FileOperations.createFileInfoForMeta(baseModelDto);
        extensionService.createMetaFileInWorkspace(metaFile).subscribe()
                .with(
                        item -> System.out.println("Meta Created")
                );

        try {
            // Instantiate a new JCodeModel
            JCodeModel codeModel = new JCodeModel();

            // Create a new package
            JPackage jp = codeModel._package(packageName);

            // Create a new class
            JDefinedClass jc = jp._class( StringUtils.capitalize(dataModelDto.getFileName()));

            // Implement Serializable
            jc._implements(Serializable.class);

            // Add Javadoc
            jc.javadoc().add("A JCodeModel class " + jc.name() + " created \n  on " + new Date(System.currentTimeMillis()));

            // Add default constructor
            jc.constructor(JMod.PUBLIC).javadoc().add("Creates a new " + jc.name() + ".");

            // Add constant serializable id
            jc.field(JMod.STATIC | JMod.FINAL, Long.class, "serialVersionUID", JExpr.lit(1L));

            // Add private variable
            List<DataModelProperty> dataModelProperties = new ArrayList<>();
            if(dataModelDto.getDataModelProperties() != null) {
                dataModelProperties = dataModelDto.getDataModelProperties();
                //Adding _id for datamodel if not present
                if(!checkForId(dataModelProperties)) {
                    DataModelProperty prop = new DataModelProperty();
                    prop.setName("_id");
                    prop.setType("String");
                    dataModelProperties.add(prop);
                }

            }

            for(DataModelProperty dataModelProperty : dataModelProperties){

                String variableName = Character.toLowerCase(dataModelProperty.getName().charAt(0)) + dataModelProperty.getName().substring(1);

                //Variable creation in case another class object
                JFieldVar jFieldVar = null;
                if (dataModelProperty.getCollectionType() != null && (dataModelProperty.getCollectionType().equalsIgnoreCase("Map") || dataModelProperty.getCollectionType().equalsIgnoreCase("HashMap"))) {
                    AbstractJClass abstractJClass = codeModel.ref(ClassProperties.findClass(dataModelProperty.getCollectionType()));
                    AbstractJClass abstractJClassObject;
                    if (Boolean.TRUE.equals(dataModelProperty.getPrimitive()) && Boolean.TRUE.equals(dataModelProperty.getValueTypePrimitive())) {
                        abstractJClassObject = abstractJClass.narrow(ClassProperties.findClass(dataModelProperty.getType()), ClassProperties.findClass(dataModelProperty.getValueType()));
                    } else if (Boolean.FALSE.equals(dataModelProperty.getPrimitive()) && Boolean.FALSE.equals(dataModelProperty.getValueTypePrimitive())) {
                        abstractJClassObject = abstractJClass.narrow(createClassOnTheGo.createClass(dataModelProperty.getType()), createClassOnTheGo.createClass(dataModelProperty.getValueType()));
                    } else if (Boolean.TRUE.equals(dataModelProperty.getPrimitive()) && Boolean.FALSE.equals(dataModelProperty.getValueTypePrimitive())) {
                        abstractJClassObject = abstractJClass.narrow(ClassProperties.findClass(dataModelProperty.getType()), createClassOnTheGo.createClass(dataModelProperty.getValueType()));
                    } else if (Boolean.FALSE.equals(dataModelProperty.getPrimitive()) && Boolean.TRUE.equals(dataModelProperty.getValueTypePrimitive())) {
                        abstractJClassObject = abstractJClass.narrow(createClassOnTheGo.createClass(dataModelProperty.getType()), ClassProperties.findClass(dataModelProperty.getValueType()));
                    } else {
                        abstractJClassObject = abstractJClass.narrow(ClassProperties.findClass(dataModelProperty.getType()), ClassProperties.findClass(dataModelProperty.getValueType()));
                    }
                    jFieldVar = jc.field(JMod.PRIVATE, abstractJClassObject, variableName);
                } else {
                    if (Boolean.FALSE.equals(dataModelProperty.getPrimitive())) {
                        if (dataModelProperty.getCollectionType() != null && Boolean.TRUE.equals(multipleRecordsCheck(dataModelProperty.getCollectionType()))) {
                         /*   AbstractJClass abstractJClass = codeModel.ref(ClassProperties.findClass(dataModelProperty.getCollectionType()));
                            //AbstractJClass abstractJClassObject = abstractJClass.narrow(createClassOnTheGo.createClass(dataModelProperty.getType()));
                             AbstractJClass externalClassRef = codeModel.ref("io.intelliflow.generated.models."+dataModelProperty.getType());
				jFieldVar = jc.field(JMod.PRIVATE, externalClassRef, variableName);
                        */
			
                            //Collection Type identified List, HashMap, etc
                        /*    AbstractJClass abstractJClass = codeModel.ref(ClassProperties.findClass(dataModelProperty.getCollectionType()));
                            //Generate the class to be set inside the Collection
                            JDefinedClass collectionObjectTypeClass = codeModel._class("io.intelliflow.generated.models."+dataModelProperty.getType());
                            //Add the class type to the collection
                            AbstractJClass abstractJClassObject = abstractJClass.narrow(collectionObjectTypeClass);
                            //Set the variable in the top most class wrapper
                            jFieldVar = jc.field(JMod.PRIVATE, abstractJClassObject, variableName);
			*/

                            AbstractJClass abstractJClassObject = null;

  			   try{
                            //Collection Type identified List, HashMap, etc
                            AbstractJClass abstractJClass = codeModel.ref(ClassProperties.findClass(dataModelProperty.getCollectionType()));
                            //Generate the class to be set inside the Collection
                            JDefinedClass collectionObjectTypeClass = codeModel._getClass("io.intelliflow.generated.models."+dataModelProperty.getType());
                            if(null == collectionObjectTypeClass){

                                 collectionObjectTypeClass = codeModel._class("io.intelliflow.generated.models."+dataModelProperty.getType());

                            }
                                //Add the class type to the collection
                            abstractJClassObject = abstractJClass.narrow(collectionObjectTypeClass);                            

                            }catch(JClassAlreadyExistsException exception){
                                exception.printStackTrace();;
                            }

                            if(abstractJClassObject != null){
                                  //Set the variable in the top most class wrapper
                            jFieldVar = jc.field(JMod.PRIVATE, abstractJClassObject, variableName);
                            }

		} else {
                            jFieldVar = jc.field(JMod.PRIVATE, createClassOnTheGo.createClass(dataModelProperty.getType()), variableName);
                        }
                    } else {
                        if (dataModelProperty.getCollectionType() != null && Boolean.TRUE.equals(multipleRecordsCheck(dataModelProperty.getCollectionType()))) {
                            AbstractJClass abstractJClass = codeModel.ref(ClassProperties.findClass(dataModelProperty.getCollectionType()));
                            AbstractJClass abstractJClassObject = abstractJClass.narrow(ClassProperties.findClass(dataModelProperty.getType()));
                            jFieldVar = jc.field(JMod.PRIVATE, abstractJClassObject, variableName);
                        } else {
                            jFieldVar = jc.field(JMod.PRIVATE, ClassProperties.findClass(dataModelProperty.getType()), variableName);
                        }
                    }
                }

                if(dataModelProperty.getMandatory()) {
                    jFieldVar.annotate(NotNull.class);
                }
                JMethod getter;
                if(dataModelProperty.getType().equalsIgnoreCase("Boolean")) {
                     getter = jc.method(JMod.PUBLIC, jFieldVar.type(), "is".concat(StringUtils.capitalize(variableName)));
                }else {
                     getter = jc.method(JMod.PUBLIC, jFieldVar.type(), "get".concat(StringUtils.capitalize(variableName)));
                }
                getter.body()._return(jFieldVar);
                JMethod setter = jc.method(JMod.PUBLIC, codeModel.VOID, "set".concat(StringUtils.capitalize(variableName)));
                setter.param(jFieldVar.type(), jFieldVar.name());
                setter.body().assign(JExpr._this().ref(jFieldVar.name()), JExpr.ref(jFieldVar.name()));

            }

           // creating file at temp directory
            Path tempPath = Files.createTempDirectory("datamodel");
            new JCMWriter(codeModel).build(tempPath.toFile());

            //create file info object to store in repo -------------------------
            FileInformation fileInfo = new FileInformation();
            fileInfo.setFileName(dataModelDto.getFileName()  + ".java");
            fileInfo.setFileType("datamodel");
            fileInfo.setContent(Files.readString(Path.of(tempPath + "/io/intelliflow/generated/models/" + dataModelDto.getFileName() + ".java"), StandardCharsets.US_ASCII));
            fileInfo.setWorkspaceName(dataModelDto.getWorkspaceName());
            fileInfo.setMiniApp(dataModelDto.getMiniAppName());
            fileInfo.setUserId(userId);
            fileInfo.setComment("Created data model " + dataModelDto.getFileName());
            fileInfo.setUserId(userId);
            FileOperations.deleteFiles(tempPath);
            return extensionService.createFileInRepo(fileInfo);
            //--------------------------------------------------------

        } catch (JCodeModelException e) {
            Log.error("JCodeModel Creating Failed:::");
            e.printStackTrace();
        } catch (IOException e) {
            Log.error("Class Creating Failed::: JCMWriter build failed");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public Uni<EventResponseModel> bind(BasePropertyDto basePropertyDto) {
       return null;
    }

    /*
        Function to generate a data model from specified form file
        Returns a data model with the properties assigned
     */
    private DataModelDto generateModelFromForm(File tempFormFile, DataModelDto newDataModel) {
        try {
            String jsondata = Files.readString(tempFormFile.toPath());
            JSONArray jArray = new JSONArray(jsondata);
            List<DataModelProperty> modelProperties = new ArrayList<DataModelProperty>();
            for(int i = 0; i < jArray.length(); i++) {
                JSONObject jsonobj = (JSONObject) jArray.get(i);
                DataModelProperty modelProperty = new DataModelProperty();
                modelProperty.setName(jsonobj.get("fieldName").toString());
                //TODO: Map the element types to corresponding data types
                // or accept as param from front end
                modelProperty.setType("String"); //hardcoded for now
                //System.out.println("Element Type: " +  jsonobj.get("elementType"));
                modelProperties.add(modelProperty);
            }
            newDataModel.setDataModelProperties(modelProperties);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return newDataModel;
    }

    @Override
    public Uni<EventResponseModel> dataExtractor(FileModelDto fileModelDto) {
        return null;
    }

    private Boolean multipleRecordsCheck(String type) {
        switch (type) {
            case "List":
            case "LinkedList":
            case "HashMap":
            case "Map":
            case "ArrayList":
            case "Set":
            case "Deque":
                return true;

            default:
                return false;
        }
    }
    @Override
    public Uni<EventResponseModel> createMultiple(BaseModelList baseModels, String flag, String userId) {
        EventResponseModel response = new EventResponseModel();
        for(BaseDataModelDTO datamodel : baseModels.getBaseModels()) {
            try {
                createFile(datamodel, userId).subscribe()
                        .with(
                                i -> System.out.println("DataModel File " + datamodel.getFileName() + " Created")
                        );
            } catch (CustomException e) {
                throw new RuntimeException(e);
            }
        }
        response.setMessage("Success");
        return Uni.createFrom().item(() -> response);
    }

    @Override
    public Uni<EventResponseModel> bindMultiple(BindPropertyList propertyList) {
        return null;
    }

    private boolean checkForId(List<DataModelProperty> properties) {
        for(DataModelProperty prop : properties) {
            if(prop.getName().equals("_id")) {
                return true;
            }
        }
        return false;
    }

}
