package org.alfresco.bm.event.selector;

import java.util.StringTokenizer;

public class EventSuccessorInfo
{
    private String eventName;
    private int weighting;
    private Long delay;
    
    public EventSuccessorInfo(String eventName, Integer weightingOverride)
    {
        this(eventName, null, weightingOverride, null);
    }

    public EventSuccessorInfo(String eventName, String weightingsStr)
    {
        this(eventName, weightingsStr, null, null);
    }

    public EventSuccessorInfo(String eventName, String weightings, Integer weightingOverride)
    {
        this(eventName, weightings, weightingOverride, null);
    }

    public EventSuccessorInfo(String eventName, String weightings, Integer weightingOverride, Long delay)
    {
        super();
        this.eventName = eventName.trim();
        if(weightings != null)
        {
            this.weighting = parseWeightings(weightings);
        }
        if(weightingOverride != null)
        {
            this.weighting += weightingOverride.intValue();
        }
        this.delay = delay;
    }
    
    private int parseWeightings(String weightings)
    {
        int weighting = 1;

        StringTokenizer st = new StringTokenizer(weightings, ",");
        while(st.hasMoreTokens())
        {
            String weighingStr = st.nextToken().trim();
            weighting *= Integer.parseInt(weighingStr);
        }

        return weighting;
    }

    public String getEventName()
    {
        return eventName;
    }

    public int getWeighting()
    {
        return weighting;
    }

    public Long getDelay()
    {
        return (delay == null ? 0l : delay);
    }

    @Override
    public String toString()
    {
        return "EventSuccessorInfo [eventName=" + eventName + ", weighting="
                + weighting + ", delay=" + delay + "]";
    }
}
