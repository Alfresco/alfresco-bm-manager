package org.alfresco.bm.manager.api.v1;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler
{

    protected final Log logger = LogFactory.getLog(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ErrorMessage> handleExceptionCustom(Exception ex, WebRequest request)
    {

        ErrorMessage exceptionResponse = new ErrorMessage(ex.getMessage(), ex.getCause().getMessage());
        logger.error(ex.getMessage());

        return new ResponseEntity<ErrorMessage>(exceptionResponse, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}

class ErrorMessage
{
    private String message;
    private String details;

    public ErrorMessage(String message, String details)
    {
        super();
        this.message = message;
        this.details = details;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getDetails()
    {
        return details;
    }

    public void setDetails(String details)
    {
        this.details = details;
    }
}
