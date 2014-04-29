package org.alfresco.bm.event.selector;
import org.alfresco.bm.event.Event;

/**
 * Selects the next event from a set of configured event successors to the current event. 
 * 
 * @author steveglover
 * @since 1.3
 */
public interface EventSelector
{
    /**
     * The event selector's name, may be null.
     * 
     * @return the event selector's name
     */
    String getName();
    
    /**
     * Select next event, which may be "noop" indicating that the event processing should end.
     * Check that the event is a valid event by looking it up in the registry (does it have a
     * bean definition?).
     * 
     * @param input         the input into the previous event
     * @param response      the response from the previous event
     * @return              the next event, or <tt>null</tt> if there is no successor.
     * 
     * @throws Exception
     */
    Event nextEvent(Object input, Object response) throws Exception;

    /**
     * The number of event successors registered.
     * @return the number of event successors
     */
    int size();
}
