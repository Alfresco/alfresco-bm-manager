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
package org.alfresco.bm.event.producer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * A registry of {@link EventProducer event producers} that allow for simple
 * {@link AbstractEventProcessor#register() registration}
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class EventProducerRegistry
{
    private final Map<String, EventProducer> producers;
    private final ReadLock readLock;
    private final WriteLock writeLock;
    
    public EventProducerRegistry()
    {
        this.producers = new HashMap<String, EventProducer>(97);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }
    
    /**
     * Register a producer for a given event name
     * 
     * @param eventName         the name of the event
     * @param producer          the event producer for the event name
     */
    public void register(String eventName, EventProducer producer)
    {
        writeLock.lock();
        try
        {
            producers.put(eventName, producer);
        }
        finally
        {
            writeLock.unlock();
        }
    }
    
    /**
     * Get the producer that must handle events with the given name
     * 
     * @param eventName         the name of the event to be processed
     * @return                  a producer for the event or <tt>null</tt> if it is unmapped
     */
    public EventProducer getProducer(String eventName)
    {
        readLock.lock();
        try
        {
            return producers.get(eventName);
        }
        finally
        {
            readLock.unlock();
        }
    }
}
