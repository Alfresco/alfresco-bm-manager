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
package org.alfresco.bm.driver.event.selector;

import org.alfresco.bm.driver.event.Event;

/**
 * Selects the next event from a set of configured event successors to the current event. 
 *  
 * @author Steve Glover
 * @since 1.3
 */
public interface EventSelector
{
    /**
     * The event selector's name, may be null.
     * 
     * @return the event selector's name
     */
    String getName();
    
    /**
     * Select next event, which may be "noop" indicating that the event processing should end.
     * Check that the event is a valid event by looking it up in the registry (does it have a
     * bean definition?).
     * 
     * @param input         the input into the previous event
     * @param response      the response from the previous event
     * @return              the next event, or <tt>null</tt> if there is no successor.
     * 
     * @throws Exception
     */
    Event nextEvent(Object input, Object response) throws Exception;

    /**
     * The number of event successors registered.
     * @return the number of event successors
     */
    int size();
}
