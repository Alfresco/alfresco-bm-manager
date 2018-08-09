/*
 * #%L
 * Alfresco Benchmark Manager
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */
package org.alfresco.bm.driver.event.selector;

import org.alfresco.bm.driver.event.Event;
import org.alfresco.bm.driver.event.EventProcessor;
import org.alfresco.bm.driver.event.EventProcessorRegistry;
import org.alfresco.bm.driver.event.selector.EventDataObject.STATUS;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract base class for event selectors. 
 *  
 * @author Steve Glover
 * @since 1.3
 */
public abstract class AbstractEventSelector implements EventSelector
{
    protected String name;
    protected EventProcessorRegistry registry;
    
    /** Logging */
    protected Log logger = LogFactory.getLog(this.getClass());

    public AbstractEventSelector(String name, EventProcessorRegistry registry)
    {
        super();
        this.name = name;
        this.registry = registry;
    }
    
    public AbstractEventSelector(EventProcessorRegistry registry)
    {
        this(null, registry);
    }

    public String getName()
    {
        return name;
    }

    /**
     * Implemented by subclasses to select a successor event.
     * 
     * @param input         the input to the previous event
     * @param response      the response from the previous event
     * @return              the successor event details
     */
    protected abstract EventSuccessor next(Object input, Object response);

    @Override
    public Event nextEvent(Object input, Object response) throws Exception
    {
        Event nextEvent = null;

        if (size() > 0)
        {
            // Choose the next event processor
            EventSuccessor eventSuccessor = next(input, response);
            
            String nextEventName = eventSuccessor.getEventName();
            EventDataObject nextEventInput = null;
            
            if (logger.isDebugEnabled())
            {
            	logger.debug("AbstractEventSelector -> nextEventName: " + nextEventName);
            }
    
            if (nextEventName != null && !nextEventName.equals("") && !nextEventName.equalsIgnoreCase("noop"))
            {
                EventProcessor eventProcessor = registry.getProcessor(nextEventName);
                if (eventProcessor == null)
                {
                    throw new RuntimeException(
                            "Event selector contains unknown event mapping: " + nextEventName + "." +
                            "No further events will be published.");
                }
                else if (!(eventProcessor instanceof EventDataCreator))
                {
                    // The next processor wired in cannot create its own input data.
                    // We assume that the event data can be passed directly in.
                    nextEventInput = new EventDataObject(STATUS.SUCCESS, response);
                }
                else
                {
                    EventDataCreator eventDataCreator = (EventDataCreator)eventProcessor;
                    nextEventInput = eventDataCreator.createDataObject(input, response);
                }
            }

            if(nextEventInput != null && nextEventInput.getStatus().equals(EventDataObject.STATUS.SUCCESS))
            {
                // Construct the event with the new data and an appropriate delay
                nextEvent = new Event(
                        nextEventName,
                        System.currentTimeMillis(),
                        nextEventInput.getData(), true);
                
                if (logger.isDebugEnabled())
                {
                	logger.debug("AbstractEventSelector: next event created.");
                }
            }
        }

        // Done
        return nextEvent;
    }
}
