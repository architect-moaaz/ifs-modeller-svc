package io.intelliflow.utils;

import io.intelliflow.centralCustomExceptionHandler.CustomException;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.RestResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ResponseUtils {

    public static Uni<RestResponse<EventResponseModel>> validateResponse(Uni<EventResponseModel> responseUni){
        return responseUni.onItem().transform(
                item -> RestResponse.ResponseBuilder.create(Response.Status.OK, item).build()
        ).onFailure().recoverWithItem(
                call -> {
                    EventResponseModel model = new EventResponseModel();
                    CustomException exception = (CustomException) call;
                    model.setMessage(exception.getResponse().getHeaderString("Error"));
                    return RestResponse.ResponseBuilder.create(exception.getResponse().getStatusInfo(), model).
                            type(MediaType.APPLICATION_JSON).
                            build();
                }
        );
    }

    //Method to handle exception thrown from modeller
    public static Uni<RestResponse<EventResponseModel>> inHouseExceptionH(Exception e){
        return Uni.createFrom().item(() -> {
            EventResponseModel model = new EventResponseModel();
            model.setMessage(e.getMessage());
            return RestResponse.ResponseBuilder.create(RestResponse.Status.CONFLICT, model).
                    type(MediaType.APPLICATION_JSON).
                    build();
        });
    }

}
