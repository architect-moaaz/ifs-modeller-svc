package io.intelliflow.service.rest;

import io.intelliflow.dto.model.FileModelDto;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.dto.repomanager.FileInformation;
import io.intelliflow.service.repomanager.ExtensionService;
import io.intelliflow.utils.ResponseUtils;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Path("modellerService")
@Produces(MediaType.APPLICATION_JSON)
public class WorkspaceRestService {

    @Inject
    @RestClient
    ExtensionService extensionService;

    @HeaderParam("user") String userId = "admin";
    @POST
    @Path("/createWorkspace")
    public Uni<RestResponse<EventResponseModel>> createWorkspace(FileModelDto fileModelDto) {
        FileInformation fileInformation = new FileInformation();
        fileInformation.setWorkspaceName(fileModelDto.getWorkspaceName());
        fileInformation.setUserId(userId);
        return ResponseUtils.validateResponse(extensionService.createWorkspace(fileInformation));
    }

    @GET
    @Path("/listWorkspaces")
    public Uni<RestResponse<EventResponseModel>> listWorkspaces() {
        return ResponseUtils.validateResponse(extensionService.listWorkspaces());
    }

    @DELETE
    @Path("/deleteWorkspace")
    public Uni<RestResponse<EventResponseModel>> deleteWorkspace(FileModelDto fileModelDto) {
        FileInformation fileInformation = new FileInformation();
        fileInformation.setWorkspaceName(fileModelDto.getWorkspaceName());
        fileInformation.setUserId(userId);
        return ResponseUtils.validateResponse(extensionService.deleteWorkspace(fileInformation));
    }

    @GET
    @Path("/{workspacename}/data")
    public Uni<RestResponse<EventResponseModel>> getAppDataForWorkspace(
            @PathParam("workspacename") String workspaceName,
            @QueryParam("sort") String sortCriteria,
            @QueryParam("filter") String filters,
            @QueryParam("status") String status,
            @DefaultValue("1") @QueryParam("page") int pageNumber,
            @DefaultValue("3000") @QueryParam("size") int pageSize) {
        String encodedSortCriteria = null;
        String encodedFilter = null;
        if(sortCriteria != null && !sortCriteria.isEmpty()) {
            encodedSortCriteria = URLEncoder.encode(sortCriteria, StandardCharsets.UTF_8);
            encodedSortCriteria = encodedSortCriteria.replace("+", "%20");
        }
        if(filters != null && !filters.isEmpty()) {
            encodedFilter = URLEncoder.encode(filters, StandardCharsets.UTF_8);
            encodedFilter = encodedFilter.replace("+", "%20");
        }
        return ResponseUtils.validateResponse(extensionService.getAppDataForWorkspace(workspaceName, status, pageNumber, pageSize, encodedSortCriteria, encodedFilter));
    }
}
