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
package org.alfresco.bm.driver.event;

import org.alfresco.bm.common.EventResult;
import org.alfresco.bm.common.ResultService;

/**
 * Simple barrier that reschedules itself until a number of eventresults ({@link RecordedEvent}s) are
 * available. If the number is reached, an event is scheduled whose name is equal to the value of the property
 * 'nextEventName'.
 * 
 * <h1>Input</h1>
 * 
 * No input required.
 * 
 * <h1>Data</h1>
 * 
 * Uses the {@link ReportService} as data-input.
 * 
 * <h1>Actions</h1>
 * 
 * Periodic check is performed to see if number is reached. If not, event is scheduled.
 * 
 * <h1>Output</h1>
 * 
 * If number is reached: {@link #getNextEventName()}, if not: reschedules same event.
 * 
 * @author Frederik Heremans
 */
public class ResultBarrier extends AbstractEventProcessor
{
    private static final long DEFAULT_INTERVAL = 1000;
    
    private String nextEventName;
    private String countEventName;
    private long expectedCount;
    private long checkInterval;
    private ResultService resultService;
    
    /**
     * Create a new barrier.
     * 
     * @param nextEventName name of the event to queue when barrier is released.
     * @param eventService event service
     */
    public ResultBarrier(String countEventName, long expectedCount, String nextEventName, ResultService resultService)
    {
        this.nextEventName = nextEventName;
        this.resultService = resultService;
        this.countEventName = countEventName;
        this.expectedCount = expectedCount;
        
        checkInterval = DEFAULT_INTERVAL;
    }

    @Override
    public EventResult processEvent(Event event)
    {
        long count = resultService.countResultsByEventName(countEventName);
        if(count < expectedCount)
        {
            long scheduledTime = System.currentTimeMillis() + checkInterval;
            Event reschedule = new Event(event.getName(), scheduledTime, null);
            
            return new EventResult(
                    "Not enough results for '" + countEventName + "' (" +  count + "/" + expectedCount +
                    "), barrier not releasing",
                    reschedule);
        }
        else
        {
            return new EventResult(
                    "Enough results for '" + countEventName + "', barrier released", 
                    new Event(getNextEventName(), System.currentTimeMillis(), null));
        }
    }
    
    /**
     * @param nextEventName name of the event to queue when number is reached.
     */
    public void setNextEventName(String nextEventName)
    {
        this.nextEventName = nextEventName;
    }
    
    /**
     * @return name of the event to queue when number is reached.
     */
    public String getNextEventName()
    {
        return this.nextEventName;
    }
    
    /**
     * Set the interval this event is scheduled to check if barrier can be released. 
     * 
     * @param checkInterval interval in millis
     */
    public void setCheckInterval(long checkInterval)
    {
        this.checkInterval = checkInterval;
    }
}
