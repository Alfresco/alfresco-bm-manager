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
package org.alfresco.bm.driver.event.selector;

/**
 * Implemented by event processors using the event selector framework to generate input for the next event.
 *  
 * @author Steve Glover
 * @since 1.3
 */
public interface EventDataCreator
{
    /**
     * Create a data object for use in a specific api call and return it. The data
     * can be based on the 'response' of the previous request, if required.
     * 
     * @param input         the input into the previous event, may be null
     * @param response      the response from the previous event, may be null
     * @return              the input into the next event, should not ne null
     *                      
     */
    EventDataObject createDataObject(Object input, Object response) throws Exception;
}
