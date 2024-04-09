package io.intelliflow.centralCustomExceptionHandler;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class CustomExceptionHandler implements ResponseExceptionMapper<Exception> {
    @Override
    public Exception toThrowable(Response response) {
        return new CustomException(response);
    }

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return ResponseExceptionMapper.super.handles(status, headers);
    }

    @Override
    public int getPriority() {
        return ResponseExceptionMapper.super.getPriority();
    }

}
