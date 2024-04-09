package io.intelliflow.service.repomanager;

import io.intelliflow.dto.model.MessageEventModel;
import io.intelliflow.dto.repomanager.AppupdateInformation;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.dto.repomanager.FileInformation;
import io.intelliflow.dto.repomanager.TemplateInformation;
import io.intelliflow.model.db.AppTemplate;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;

@RegisterRestClient(configKey = "repository-api")
public interface ExtensionService {


    @POST
    @Path("/workspace")
    Uni<EventResponseModel> createWorkspace(FileInformation fileInformation);

    @GET
    @Path("/workspace")
    Uni<EventResponseModel> listWorkspaces();

    @GET
    @Path("/workspace/{workspacename}/data")
    Uni<EventResponseModel> getAppDataForWorkspace(
            @PathParam("workspacename") String workspaceName,
            @QueryParam("status") String status,
            @QueryParam("page") int pageNumber,
            @QueryParam("size") int pageSize,
            @QueryParam("sort") String sortCriteria,
            @QueryParam("filter") String filters);

    @DELETE
    @Path("/workspace")
    Uni<EventResponseModel> deleteWorkspace(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository")
    Uni<EventResponseModel> createMiniApp(FileInformation fileInformation);

    @GET
    @Path("/workspace/repository")
    Uni<EventResponseModel> listMiniApps(FileInformation fileInformation);

    @DELETE
    @Path("/workspace/repository")
    Uni<EventResponseModel> deleteRepository(FileInformation fileInformation);

    @GET
    @Path("/workspace/repository/{workspacename}/{appname}/data")
    Uni<EventResponseModel> getFileDataForApp(
            @PathParam("workspacename") String workspaceName,
            @PathParam("appname") String appName,
            @QueryParam("status") String status
    );

    @POST
    @Path("/workspace/repository/resource")
    Uni<EventResponseModel> createFileInRepo(FileInformation fileInformation);

    @GET
    @Path("/workspace/repository/resource")
    Uni<EventResponseModel> fetchFileFromRepository(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository/resource/asset/list")
    Uni<EventResponseModel> getAllResources(FileInformation fileInformation);

    @DELETE
    @Path("/workspace/repository/resource")
    Uni<EventResponseModel> deleteFileInRepo(FileInformation fileInformation);

    @PATCH
    @Path("/workspace/repository/resource")
    Uni<EventResponseModel> updateFileInRepository(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository/resource/file/saveMeta")
    Uni<EventResponseModel> createMetaFileInWorkspace(FileInformation fileInformation);

    @GET
    @Path("/workspace/repository/resource/file/fetchMeta")
    Uni<EventResponseModel> fetchMetaContentFromRepository(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository/resource/asset/draft")
    Uni<EventResponseModel> saveAsDraft(FileInformation fileInformation);

    @GET
    @Path("/workspace/repository/resource/asset/draft/FETCH")
    Uni<EventResponseModel> fetchDraftList(FileInformation fileInformation);

    @GET
    @Path("/workspace/repository/resource/asset/draft/LOAD")
    Uni<EventResponseModel> loadDraft(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository/resource/asset/lock")
    Uni<EventResponseModel> lockAsset(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository/resource/asset/release")
    Uni<EventResponseModel> releaseAsset(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository/resource/bpmn/validate")
    Uni<EventResponseModel> validateBpmn(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository/resource/dmn/validate")
    Uni<EventResponseModel> validateDmn(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository/resource/rename")
    Uni<EventResponseModel> renameFile(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository/resource/versionList")
    Uni<EventResponseModel> gitTag(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository/resource/revert")
    Uni<EventResponseModel> revertFile(FileInformation fileInformation);

    @POST
    @Path("/workspace/repository/cloneApplication")
    Uni<EventResponseModel> cloneApplication(TemplateInformation templateInformation);

    @POST
    @Path("/workspace/repository/cloneFile")
    Uni<EventResponseModel> cloneFile(TemplateInformation templateInformation);

    @POST
    @Path("/workspace/repository/updateApp")
    Uni<EventResponseModel> updateApplication(AppupdateInformation appupdateInformation);

    @POST
    @Path("/workspace/repository/resource/messageEvent")
    Uni<EventResponseModel> updatePropertiesForMessage(MessageEventModel model);

    @POST
    @Path("/template")
    Uni<EventResponseModel> addTemplate(AppTemplate appTemplate);

    @PUT
    @Path("template/{templateName}")
    Uni<EventResponseModel> updateTemplate(
            @PathParam("templateName") String templateName, AppTemplate updatedTemplate);

    @DELETE
    @Path("template/{templateName}")
    Uni<EventResponseModel> deleteTemplate(@PathParam("templateName") String templateName);

    @GET
    @Path("template/{templateName}")
    Uni<EventResponseModel> getTemplate(@PathParam("templateName") String templateName);

    @GET
    @Path("workspace/repository/resource/buildHistory")
    Uni<EventResponseModel> getBuildHistory(@HeaderParam("workspace") String workspace,
                                            @HeaderParam("appName") String app,
                                            @HeaderParam("Timezone") String timeZone,
                                            @QueryParam("page") int pageNumber,
                                            @QueryParam("size") int pageSize);
}
