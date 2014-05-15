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
package org.alfresco.bm.event;


/**
 * Interface for classes that handle persistence and retrieval of {@link Event events}.
 * <p/>
 * Note that the <i>next</i> event is the event that should be processed
 * next and not necessarily the event at the head of the queue.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public interface EventService
{
    /**
     * @return                      Returns the number of events in the queue
     */
    long count();
    
    /**
     * Adds an event to the provider. This is done to populate the queue with
     * a new event to be processed.
     * 
     * @param event             the event to add (the {@link Event#getId() event ID} must be <tt>null</tt>
     * @return                  the ID of the event
     */
    String putEvent(Event event);
    
    /**
     * Retrieve an event by ID
     * 
     * @param id                the ID of the event as given in {@link #putEvent(Event) put}
     * @return                  the event by ID or <tt>null</tt> if it does not exist
     */
    Event getEvent(String id);
    
    /**
     * Retrieve the next event in the queue.
     * <p/>
     * This method must update the event queue time to the current time atomically.
     * 
     * @param serverId              the identifier of the server taking the event (never <tt>null</tt>)
     * @param latestScheduledTime   the maximum scheduled time for events
     * @param localDataOnly         <tt>true</tt> to limit searches to events that have server-specific
     *                              data matching the given server ID
     * @return                      Returns the next event in the queue or <tt>null</tt>
     */
    Event nextEvent(String serverId, long latestScheduledTime, boolean localDataOnly);
    
    /**
     * Delete an event from the provider.  This can be done after the event has
     * been fully processed and there is no further requirement for it.
     * <p/>
     * <b>NOTE: The data cannot be accessed after this event.</b>
     * 
     * @param event             the event to delete
     * @return                  <tt>true</tt> if the event was deleted otherwise <tt>false</tt>
     */
    boolean deleteEvent(Event event);
}
