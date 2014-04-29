package org.alfresco.bm.event.selector;

public class EventDataObject
{
    public static enum STATUS
    {
        SUCCESS,      // we have data
        INVALIDINPUT, // the input was invalid in some way e.g. null
        INPUT_NOT_AVAILABLE; // not possible to get input data from the previous response
    };
    
    private STATUS status;
    private Object data;
    
    public EventDataObject(STATUS status, Object data)
    {
        super();
        this.status = status;
        this.data = data;
    }
    
    public STATUS getStatus()
    {
        return status;
    }
    
    public Object getData()
    {
        return data;
    }
}
