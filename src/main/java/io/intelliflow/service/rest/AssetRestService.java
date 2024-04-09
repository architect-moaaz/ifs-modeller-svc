package io.intelliflow.service.rest;

import io.intelliflow.centralCustomExceptionHandler.CustomException;
//import io.intelliflow.dao.TaskFormMapDao;
import io.intelliflow.dto.bindproperty.BasePropertyDto;
import io.intelliflow.dto.bindproperty.BindPropertyList;
import io.intelliflow.dto.model.*;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.dto.repomanager.FileInformation;
import io.intelliflow.factory.GetModellerFactory;
import io.intelliflow.model.CustomFormDataDTO;
import io.intelliflow.model.db.TaskFormMap;
import io.intelliflow.modeller.BaseModeller;
import io.intelliflow.repository.TaskFormMapRepository;
import io.intelliflow.service.FormToBpmnMapService;
import io.intelliflow.service.repomanager.ExtensionService;
import io.intelliflow.support.ModellerNameException;
import io.intelliflow.utils.ResponseUtils;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.RestResponse;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

@Path("modellerService")
public class
AssetRestService {

    @Inject
    GetModellerFactory modellerFactory;

    @Inject
    private TaskFormMapRepository taskFormMapRepository;

    @Inject
    @RestClient
    ExtensionService extensionService;
    @HeaderParam("user") String userId = "admin";

    @POST
    @Path("/{modeller}/createFile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<RestResponse<EventResponseModel>> createFile(@PathParam("modeller")String modeller, DataModelDto dataModelDto) {
        try {
            BaseModeller baseModeller = modellerFactory.getModeller(GetModellerFactory.Modellers.valueOf(modeller.toUpperCase()));
            return ResponseUtils.validateResponse(baseModeller.createFile(dataModelDto, userId));
        }catch (Exception e){
            return ResponseUtils.inHouseExceptionH(e);
        }
    }


    @POST
    @Path("/{modeller}/createFile/blob")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<RestResponse<EventResponseModel>> createFileBlob(@PathParam("modeller") String modeller, @MultipartForm CustomFormDataDTO form) throws ModellerNameException, IOException {
        try {
            BaseModeller baseModeller = modellerFactory.getModeller(GetModellerFactory.Modellers.valueOf(modeller.toUpperCase()));
            DataModelDto dataModelDto = new DataModelDto();
            dataModelDto.setFileContent(Files.readAllBytes(form.getFile().uploadedFile().toAbsolutePath()));
            dataModelDto.setWorkspaceName(form.getWorkspace());
            dataModelDto.setMiniAppName(form.getAppName());
            dataModelDto.setFileName(form.getFileName());
            dataModelDto.setFileType(form.getFileType());
            return ResponseUtils.validateResponse(baseModeller.createFile(dataModelDto, userId));
        } catch (Exception e) {
            return ResponseUtils.inHouseExceptionH(e);
        }
    }

    @POST
    @Path("/{modeller}/updateFile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<EventResponseModel> updateFile(@PathParam("modeller")String modeller, DataModelDto dataModelDto) throws ModellerNameException {
        BaseModeller baseModeller = modellerFactory.getModeller(GetModellerFactory.Modellers.valueOf(modeller.toUpperCase()));
        return baseModeller.updateFile(dataModelDto);
    }

    @POST
    @Path("/{modeller}/generateFile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<EventResponseModel> generateFile(@PathParam("modeller")String modeller,
                                                @QueryParam("flag") String flag, DataModelDto dataModelDto) throws ModellerNameException {
        BaseModeller baseModeller = modellerFactory.getModeller(GetModellerFactory.Modellers.valueOf(modeller.toUpperCase()));
        return baseModeller.generateFile(dataModelDto, flag, userId);
    }

    @POST
    @Path("/fetchFile/{type}")
    public Uni<EventResponseModel> fetchFile(FileModelDto fetchModel, @PathParam("type")String type) {
        FileInformation fileInfo = new FileInformation();
        fileInfo.setWorkspaceName(fetchModel.getWorkspaceName());
        fileInfo.setMiniApp(fetchModel.getMiniAppName());
        fileInfo.setFileType(fetchModel.getFileType());
        fileInfo.setFileName(fetchModel.getFileName());
        if(type.equals("content")) {
            return extensionService.fetchFileFromRepository(fileInfo);
        }
        else {
            return extensionService.fetchMetaContentFromRepository(fileInfo);
        }
    }

    @POST
    @Path("/{modellerType}/bind")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<RestResponse<EventResponseModel>> bindModeller(@PathParam("modellerType")String modellerType, BasePropertyDto basePropertyDto) throws ModellerNameException {
        BaseModeller baseModeller = modellerFactory.getModeller(GetModellerFactory.Modellers.valueOf(modellerType.toUpperCase()));
        return ResponseUtils.validateResponse(baseModeller.bind(basePropertyDto));

    }


    @DELETE
    @Path("/deleteFile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<RestResponse<EventResponseModel>>  deleteFile(FileModelDto fileModelDto) throws ModellerNameException {
        BaseModeller baseModeller = modellerFactory.getModeller(GetModellerFactory.Modellers.DATAMODELLER);
        return ResponseUtils.validateResponse(baseModeller.deleteFile(fileModelDto,userId));
    }

    @POST
    @Path("/{modeller}/getData")
    public Uni<EventResponseModel> extractData(@PathParam("modeller")String modeller, FileModelDto fileModelDto) throws ModellerNameException {
        BaseModeller baseModeller = modellerFactory.getModeller(GetModellerFactory.Modellers.valueOf(modeller.toUpperCase()));
        return baseModeller.dataExtractor(fileModelDto);
    }

    @POST
    @Path("/lockAsset")
    public Uni<EventResponseModel> lockAsset(FileInformation fileInformation){
        return extensionService.lockAsset(fileInformation);
    }

    @POST
    @Path("/releaseAsset")
    public Uni<EventResponseModel> releaseAsset(FileInformation fileInformation){
        return extensionService.releaseAsset(fileInformation);
    }

    @POST
    @Path("/{modeller}/testvalidate")
    public Uni<EventResponseModel> testValidate(@PathParam("modeller")String modeller, DataModelDto fileModelDto) throws ModellerNameException, IOException {
        BaseModeller baseModeller = modellerFactory.getModeller(GetModellerFactory.Modellers.valueOf(modeller.toUpperCase()));
        //return baseModeller.dataExtractor(fileModelDto);
        return null;
    }

    @POST
    @Path("/saveAsDraft")
    public Uni<EventResponseModel> saveAsDraft(FileModelDto fileModelDto) {
        FileInformation fileInformation = new FileInformation();
        fileInformation.setWorkspaceName(fileModelDto.getWorkspaceName());
        fileInformation.setMiniApp(fileModelDto.getMiniAppName());
        fileInformation.setFileType(fileModelDto.getFileType());
        fileInformation.setFileName(fileModelDto.getFileName());
        fileInformation.setContent(new String(fileModelDto.getFileContent()));
        fileInformation.setComment(fileModelDto.getComment());
        fileInformation.setUserId(userId);
        return extensionService.saveAsDraft(fileInformation);
    }

    @POST
    @Path("/fetchDraft")
    public Uni<EventResponseModel> fetchDraft(FileModelDto fileModelDto) {
        FileInformation fileInformation = new FileInformation();
        fileInformation.setWorkspaceName(fileModelDto.getWorkspaceName());
        fileInformation.setMiniApp(fileModelDto.getMiniAppName());
        fileInformation.setFileType(fileModelDto.getFileType());
        fileInformation.setFileName(fileModelDto.getFileName());
        fileInformation.setComment(fileModelDto.getComment());
        return extensionService.fetchDraftList(fileInformation);
    }

    @POST
    @Path("/loadDraft")
    public Uni<EventResponseModel> loadDraft(FileModelDto fileModelDto) {
        FileInformation fileInformation = new FileInformation();
        fileInformation.setWorkspaceName(fileModelDto.getWorkspaceName());
        fileInformation.setMiniApp(fileModelDto.getMiniAppName());
        fileInformation.setFileType(fileModelDto.getFileType());
        fileInformation.setFileName(fileModelDto.getFileName());
        fileInformation.setComment(fileModelDto.getComment());
        fileInformation.setFileID(fileModelDto.getFileId());
        return extensionService.loadDraft(fileInformation);
    }

    @POST
    @Path("/form/content")
    public Uni<EventResponseModel> getTaskMapContent(TaskModel taskModel) {
//        findByTaskTypeAndName
        List<TaskFormMap> taskFormMapList = taskFormMapRepository.findByTaskTypeAndName(
                taskModel.getWorkspace(),
                taskModel.getMiniapp(),
                taskModel.getTasktype(),
                taskModel.getTaskname()
        );
        FileInformation fileInformation = new FileInformation();
        fileInformation.setWorkspaceName(taskModel.getWorkspace());
        fileInformation.setMiniApp(taskModel.getMiniapp());
        fileInformation.setFileName(taskFormMapList.get(0).getFormname());
        fileInformation.setFileType("form");
        return extensionService.fetchFileFromRepository(fileInformation);
    }

    @POST
    @Path("/form/mapping")
    public Uni<EventResponseModel> formMapping(TaskModel taskModel){
        FormToBpmnMapService service = new FormToBpmnMapService();
        return service.mapFormToBpmn(taskModel, extensionService);
    }

    @GET
    @Path("/workflow/mapping/{workspaceName}/{appName}")
    public Uni<EventResponseModel> getExistingMapping(
            @PathParam("workspaceName") String workspaceName,
            @PathParam("appName") String appName){
        FormToBpmnMapService service = new FormToBpmnMapService();
        return service.getMappings(workspaceName, appName, extensionService, false);
    }

    @POST
    @Path("/form/mapping/delete")
    public Uni<EventResponseModel> deleteFormMapping(TaskModel taskModel){
        FormToBpmnMapService service = new FormToBpmnMapService();
        return service.removeMappings(taskModel, extensionService);
    }

    @POST
    @Path("/bpmn/validate")
    public Uni<EventResponseModel> validateBPMN(FileModelDto fileModel){
        FileInformation fileInfo = new FileInformation();
        if(Objects.nonNull(fileModel.getFileContent())){
            fileInfo.setContent(new String(fileModel.getFileContent()));
        } else {
            fileInfo.setWorkspaceName(fileModel.getWorkspaceName());
            fileInfo.setMiniApp(fileModel.getMiniAppName());
            fileInfo.setFileName(fileModel.getFileName());
            fileInfo.setFileType("bpmn");
        }
        return extensionService.validateBpmn(fileInfo);
    }
    @POST
    @Path("/dmn/validate")
    public Uni<EventResponseModel> validateDmn(FileModelDto fileModel){
        FileInformation fileInfo = new FileInformation();
        if(Objects.nonNull(fileModel.getFileContent())){
            fileInfo.setContent(new String(fileModel.getFileContent()));
        } else {
            fileInfo.setWorkspaceName(fileModel.getWorkspaceName());
            fileInfo.setMiniApp(fileModel.getMiniAppName());
            fileInfo.setFileName(fileModel.getFileName());
            fileInfo.setFileType("dmn");
        }
        return extensionService.validateDmn(fileInfo);
    }


    @POST
    @Path("/file/rename")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<RestResponse<EventResponseModel>> renameFile(FileModelDto fileModel){
        FileInformation fileInformation = new FileInformation();
        fileInformation.setWorkspaceName(fileModel.getWorkspaceName());
        fileInformation.setMiniApp(fileModel.getMiniAppName());
        fileInformation.setFileType(fileModel.getFileType());
        fileInformation.setFileName(fileModel.getFileName());
        fileInformation.setUpdatedName(fileModel.getUpdatedName());
        fileInformation.setUserId(userId);
        return ResponseUtils.validateResponse(extensionService.renameFile(fileInformation));
    }

    @POST
    @Path("/{modeller}/create/multi")
    public Uni<EventResponseModel> createMultipleDataModels(@PathParam("modeller")String modeller,
                                                            @QueryParam("flag") String flag, BaseModelList baseModels) throws ModellerNameException {
        BaseModeller baseModeller = modellerFactory.getModeller(GetModellerFactory.Modellers.valueOf(modeller.toUpperCase()));
        return baseModeller.createMultiple(baseModels, flag, userId);
    }

    @POST
    @Path("/{modeller}/bind/multi")
    public Uni<EventResponseModel> bindMultiple(@PathParam("modeller")String modeller, BindPropertyList propertyList) throws ModellerNameException {
        BaseModeller baseModeller = modellerFactory.getModeller(GetModellerFactory.Modellers.valueOf(modeller.toUpperCase()));
        return baseModeller.bindMultiple(propertyList);
    }

    @POST
    @Path("/page/mapping")
    public Uni<EventResponseModel> pageMapping(PageModel pageModel){
        FormToBpmnMapService service = new FormToBpmnMapService();
        return service.mapRoleToPage(pageModel, extensionService);
    }

    @GET
    @Path("/workflow/rolemapping/{workspaceName}/{appName}")
    public Uni<EventResponseModel> getRoleMapping(
            @PathParam("workspaceName") String workspaceName,
            @PathParam("appName") String appName){
        FormToBpmnMapService service = new FormToBpmnMapService();
        return service.getMappings(workspaceName, appName, extensionService, true);
    }

    @POST
    @Path("/role/mapping/delete")
    public Uni<EventResponseModel> deleteRoleMapping(PageModel pageModel) {
        FormToBpmnMapService service = new FormToBpmnMapService();
        return service.removeRoleMapping(pageModel, extensionService);
    }

    @GET
    @Path("/buildHistory")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<RestResponse<EventResponseModel>> getDeploymentHistory(@HeaderParam("appName") String appName,
                                                                      @HeaderParam("workspace") String workspace,
                                                                      @HeaderParam("Timezone") String timeZone,
                                                                      @QueryParam("page") @DefaultValue("1") int pageNumber,
                                                                      @QueryParam("size") @DefaultValue("10") int pageSize) {
        return ResponseUtils.validateResponse(extensionService.getBuildHistory(workspace, appName,timeZone,pageNumber,pageSize));
    }

}
