package org.alfresco.bm.event.selector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DataDependantEventSelector implements EventSelector
{
    private Map<String, EventSelector> eventSelectors = new HashMap<String, EventSelector>();

    public DataDependantEventSelector(List<EventSelector> selectors)
    {
        setEventSelectors(selectors);
    }

    public void setEventSelectors(List<EventSelector> selectors)
    {
        for(EventSelector selector : selectors)
        {
            eventSelectors.put(selector.getName(), selector);            
        }
    }

    @Override
    public int size()
    {
        int size = 0;

        for(EventSelector selector : eventSelectors.values())
        {
            size += selector.size();
        }

        return size;
    }
    
    public EventSelector getNamedEventSelector(String name)
    {
        return eventSelectors.get(name);
    }
}
