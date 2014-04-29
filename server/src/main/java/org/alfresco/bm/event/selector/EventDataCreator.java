package org.alfresco.bm.event.selector;

/**
 * Implemented by event processors using the event selector framework to generate input for the next event.
 * 
 * @author steveglover
 */
public interface EventDataCreator
{
    /**
     * Create a data object for use in a specific api call and return it. The data
     * can be based on the 'response' of the previous request, if required.
     * 
     * @param input         the input into the previous event, may be null
     * @param response      the response from the previous event, may be null
     * @return              the input into the next event, should not ne null
     *                      
     */
    EventDataObject createDataObject(Object input, Object response) throws Exception;
}
