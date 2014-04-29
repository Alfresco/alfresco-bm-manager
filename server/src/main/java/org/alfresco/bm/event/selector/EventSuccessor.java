package org.alfresco.bm.event.selector;

/**
 * Information on an event successor, including the event name, relative weighting and delay.
 * 
 * @author steveglover
 * @since 1.3
 */
public class EventSuccessor
{
    private final String eventName;
    private final int weighting;
    private final long delay;
    
    public EventSuccessor(String eventName, int weighting, long delay)
    {
        this.eventName = eventName;
        this.weighting = weighting;
        this.delay = delay;
    }

    public String getEventName()
    {
        return eventName;
    }

    public int getWeighting()
    {
        return weighting;
    }

    public long getDelay()
    {
        return delay;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("EventSuccessor [eventName=");
        builder.append(eventName);
        builder.append(", weighting=");
        builder.append(weighting);
        builder.append(", delay=");
        builder.append(delay);
        builder.append("]");
        return builder.toString();
    }
}
