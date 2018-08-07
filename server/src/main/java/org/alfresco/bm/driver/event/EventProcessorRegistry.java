/*
 * #%L
 * Alfresco Benchmark Framework Manager
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * A registry of {@link EventProcessor event processors} that allow for simple
 * {@link AbstractEventProcessor#register() registration}
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class EventProcessorRegistry
{
    private final Map<String, EventProcessor> processors;
    private final ReadLock readLock;
    private final WriteLock writeLock;
    
    public EventProcessorRegistry()
    {
        this.processors = new HashMap<String, EventProcessor>(97);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }
    
    /**
     * Register a processor for a given event name
     * 
     * @param eventName         the name of the event
     * @param processor         the processor that will handle the event
     */
    public void register(String eventName, EventProcessor processor)
    {
        writeLock.lock();
        try
        {
            processors.put(eventName, processor);
        }
        finally
        {
            writeLock.unlock();
        }
    }
    
    /**
     * Get the processor that must handle events with the given name
     * 
     * @param eventName         the name of the even to be processed
     * @return                  a processor for the event or <tt>null</tt> if it is unmapped
     */
    public EventProcessor getProcessor(String eventName)
    {
        readLock.lock();
        try
        {
            return processors.get(eventName);
        }
        finally
        {
            readLock.unlock();
        }
    }
}
