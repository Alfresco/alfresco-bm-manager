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
package org.alfresco.bm.driver.event;

import org.alfresco.bm.common.EventResult;
import org.alfresco.bm.driver.event.producer.RedirectEventProducer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simply emits a single event using the inbound data.
 * 
 * <h1>Input</h1>
 * 
 * Any data
 * 
 * <h1>Actions</h1>
 * 
 * No actions
 * 
 * <h1>Output</h1>
 * 
 * {@link #outputEventName}: Passes inbound data straight through<br/>
 * 
 * @author Derek Hulley
 * @since 1.4
 * @deprecated From 2.0, use the {@link RedirectEventProducer}.
 */
@Deprecated
public class RenameEventProcessor extends AbstractEventProcessor
{
    private static Log logger = LogFactory.getLog(RenameEventProcessor.class);
    
    private final String outputEventName;
    
    /**
     * Constructor with <b>essential</b> values
     * 
     * @param outputEventName           the event name to output
     */
    public RenameEventProcessor(String outputEventName)
    {
        super();
        this.outputEventName = outputEventName;
        
        // Deprecated
        logger.warn("The RenameEventProcessor is deprecated.");
    }

    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        Event nextEvent = new Event(outputEventName, event.getDataObject());
        // That's it
        EventResult result = new EventResult("Transfered data to event " + outputEventName, nextEvent);
        // Done
        return result;
    }
}
