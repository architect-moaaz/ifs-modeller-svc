package io.intelliflow.service.rest;

import io.intelliflow.dto.model.FileModelDto;
import io.intelliflow.dto.repomanager.AppupdateInformation;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.dto.repomanager.FileInformation;
import io.intelliflow.dto.repomanager.TemplateInformation;
import io.intelliflow.model.DataBaseConfig;
import io.intelliflow.service.repomanager.ExtensionService;
import io.intelliflow.utils.ResponseUtils;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

@Path("modellerService")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryRestService {

    @Inject
    @RestClient
    ExtensionService extensionService;
    @HeaderParam("user") String userId = "admin";
    @POST
    @Path("/createRepository")
    public Uni<RestResponse<EventResponseModel>> createMiniApp(FileModelDto fileModelDto) {
        FileInformation fileInformation = new FileInformation();

        fileInformation.setAppDisplayName(fileModelDto.getMiniAppName());

        fileInformation.setWorkspaceName(fileModelDto.getWorkspaceName());
        fileInformation.setMiniApp(((fileModelDto.getMiniAppName().trim()).replace(' ', '-')).toLowerCase());
        fileInformation.setDescription(fileModelDto.getDescription());
        fileInformation.setUserId(userId);
        JSONObject widObject = new JSONObject();
        JSONObject map = new JSONObject();
        try {
            widObject.put("workspaceName", fileModelDto.getWorkspaceName());
            widObject.put("appName", fileModelDto.getMiniAppName());
            if(Objects.nonNull(fileModelDto.getLogoURL())){
                widObject.put("logoURL", fileModelDto.getLogoURL());
            }
            if(Objects.nonNull(fileModelDto.getDeviceSupport())){
                widObject.put("deviceSupport", fileModelDto.getDeviceSupport());
            } else {
                widObject.put("deviceSupport", "B");
            }
            if(Objects.nonNull(fileModelDto.getColorScheme())){
                widObject.put("colorScheme", fileModelDto.getColorScheme());
            }
            map.put("mapping", new JSONArray());
            map.put("rolemapping", new JSONArray());
            widObject.put("configuration", map);
            fileInformation.setContent(widObject.toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.info("Initiating MiniApp Creation");
        return ResponseUtils.validateResponse(extensionService.createMiniApp(fileInformation));
    }

    @POST
    @Path("/listMiniApps")
    public Uni<RestResponse<EventResponseModel>> listMiniApps(FileModelDto fileModelDto) {
        FileInformation fileInformation = new FileInformation();
        fileInformation.setWorkspaceName(fileModelDto.getWorkspaceName());
        return ResponseUtils.validateResponse(extensionService.listMiniApps(fileInformation));
    }

    @POST
    @Path("/getResources")
    public Uni<EventResponseModel> getAllResources(FileModelDto fileModelDto) {
        FileInformation fileInfo = new FileInformation();
        fileInfo.setWorkspaceName(fileModelDto.getWorkspaceName());
        fileInfo.setMiniApp(fileModelDto.getMiniAppName());
        Uni<EventResponseModel> responseModelUni = extensionService.getAllResources(fileInfo);
        Log.info("Logged Data:::" + responseModelUni);
        System.out.println("Resources Data: " + responseModelUni);
        return responseModelUni;
    }

    @DELETE
    @Path("/deleteMiniApp")
    public Uni<RestResponse<EventResponseModel>> deleteMiniApp(FileModelDto fileModelDto) {
        FileInformation fileInformation = new FileInformation();
        fileInformation.setWorkspaceName(fileModelDto.getWorkspaceName());
        fileInformation.setMiniApp(fileModelDto.getMiniAppName());
        fileInformation.setUserId(userId);
        return ResponseUtils.validateResponse(extensionService.deleteRepository(fileInformation));
    }


    @GET
    @Path("/{workspacename}/{appname}/info")
    public Uni<RestResponse<EventResponseModel>> getFileDataForApp(
            @PathParam("workspacename") String workspaceName,
            @PathParam("appname") String appName,
            @QueryParam("status") String status
    ){
        return ResponseUtils.validateResponse(extensionService.getFileDataForApp(workspaceName, appName, status));
    }

    @POST
    @Path("/cloneApplication")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<EventResponseModel> cloneApplication(TemplateInformation templateInformation) {
        templateInformation.setUserId(userId);
        return extensionService.cloneApplication(templateInformation);
    }


    @POST
    @Path("/cloneFile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<EventResponseModel>  cloneFile(TemplateInformation templateInformation) {
        templateInformation.setUserId(userId);
        return extensionService.cloneFile(templateInformation);
    }

    @POST
    @Path("/updateApp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<RestResponse<EventResponseModel>> updateApplication(AppupdateInformation appupdateInformation) {
        appupdateInformation.setUserId(userId);
        return ResponseUtils.validateResponse(extensionService.updateApplication(appupdateInformation));
    }

    @POST
    @Path("/testJDBCConnection")
    public DataBaseConfig testDbConnection(DataBaseConfig dataBaseConfig) {
        String dbName = dataBaseConfig.getDbName();
        String dbUserName = dataBaseConfig.getDbUserName();
        String dbPassword = dataBaseConfig.getDbPassword();
        String dbType = dataBaseConfig.getDbType();
        String machineAddress = dataBaseConfig.getHost();
        String port = dataBaseConfig.getPort();
        String connectionString = "jdbc:" + dbType + "://" + machineAddress + ":" + port + "/" + dbName + "?useSSL=false";
        try (Connection ignored = DriverManager.getConnection(
                connectionString, dbUserName, dbPassword)) {
            dataBaseConfig.setConnectionEstablished(true);
            return dataBaseConfig;
        } catch (SQLException e) {
            e.printStackTrace();
            dataBaseConfig.setConnectionEstablished(false);
            return dataBaseConfig;
        }
    }
}

