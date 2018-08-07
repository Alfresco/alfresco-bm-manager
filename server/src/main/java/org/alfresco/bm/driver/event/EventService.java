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

import java.util.List;


/**
 * Interface for classes that handle persistence and retrieval of {@link Event events}.
 * <p/>
 * Note that the <i>next</i> event is the event that should be processed
 * next and not necessarily the event at the head of the queue.
 * 
 * @author Derek Hulley
 * @author Frank Becker
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
     * Only useful for testing: Retrieve all events with paging
     */
    List<Event> getEvents(int skip, int limit);
    
    /**
     * Retrieve the next event in the queue.  If the driver assignment is ignored, then the
     * scheduled time should be adjusted so that events for other drivers are only picked up
     * after a respectable time has passed i.e. the other drivers have been given a chance to
     * process their events but did not.
     * <p/>
     * This method must update the event queue time to the current time atomically.
     * 
     * @param driverId              the ID of the driver performing the search.  When supplied, only events
     *                              that are assigned to the driver are fetched; when <tt>null</tt>) any
     *                              other driver's events can be fetched as well.  Not that this is
     *                              independent of data afinity, which is always respected here.
     * @param latestScheduledTime   the maximum scheduled time for events
     * @param respectDataAfinity    <tt>true</tt> if the fetch should only fetch events bound to the current
     *                              test run instance.
     * @return                      Returns the next event in the queue or <tt>null</tt>
     */
    Event nextEvent(String driverId, long latestScheduledTime);
    
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
    
    /**
     * Clears all recorded data
     * 
     * @return true if success, false else
     * @since 2.1.4
     */
    boolean clear();
}
