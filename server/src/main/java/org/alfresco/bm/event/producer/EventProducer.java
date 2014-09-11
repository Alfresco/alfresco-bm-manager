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

import java.util.List;

import org.alfresco.bm.event.Event;

/**
 * Interface for components that can redirect events accoding to
 * customized logic.  The framework will attempt to find a producer for each event
 * and, if found, transform it using the {@link #getNextEvents(Event)} method.  If
 * no producer exists, then the event is persisted directly.
 * <p/>
 * Implementations choose the next event outcome and ensure the
 * correct input data is available.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public interface EventProducer
{
    public static final String PRODUCER_NAME_PREFIX = "producer.";
    
    /**
     * Construct a list of events to follow according the some scenario of business logic.
     * <p/>
     * Implementations use the contextual data available to determine the next
     * course of action. However, the <i>no-op</i> (empty list return) should
     * be determined as quickly and cheaply as possible.
     * <p/>
     * <b>NB:
     * Implementations must ensure that an adequate termination condition exists
     * and return an empty list.  If not, an infinite loop of events can ensue.</b>
     * 
     * @param event                 the event that triggered this choice
     * @param soakData              Session-specific data for the event
     * @param nextEventTime         the suggested next event time (for guidance)
     * @return                      a list of events to publish (never <tt>null</tt>) or
     *                              an empty list if the scenario cannot continue
     */
    List<Event> getNextEvents(Event event);
}
