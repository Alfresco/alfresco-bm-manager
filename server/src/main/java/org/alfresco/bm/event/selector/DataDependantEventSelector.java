/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.bm.event.selector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  
 * @author Steve Glover
 * @since 1.3
 */
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
