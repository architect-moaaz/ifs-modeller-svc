package io.intelliflow.service.rest;


import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.model.db.AppTemplate;
import io.intelliflow.service.repomanager.ExtensionService;
import io.intelliflow.utils.ResponseUtils;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("template")
@Produces(MediaType.APPLICATION_JSON)
public class AppTemplateRestService {

    @Inject
    @RestClient
    ExtensionService extensionService;

    @POST
    public Uni<RestResponse<EventResponseModel>> addAppTemplate(AppTemplate appTemplate) {
        return ResponseUtils.validateResponse(extensionService.addTemplate(appTemplate));
    }

    @GET
    @Path("/{templateName}")
    public Uni<EventResponseModel> getTemplate(@PathParam("templateName") String templateName) {
        return extensionService.getTemplate(templateName);
    }

    @PUT
    @Path("/{templateName}")
    public Uni<EventResponseModel> updateTemplate(@PathParam("templateName") String templateName, AppTemplate appTemplate) {
        return extensionService.updateTemplate(templateName,appTemplate);
    }

    @DELETE
    @Path("/{templateName}")
    public Uni<EventResponseModel> deleteTemplate(@PathParam("templateName") String templateName) {
        return extensionService.deleteTemplate(templateName);
    }
}

