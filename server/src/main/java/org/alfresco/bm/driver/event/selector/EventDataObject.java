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
 *  
 * @author Steve Glover
 * @since 1.3
 */
public class EventDataObject
{
    public enum STATUS
    {
        SUCCESS,      // we have data
        INVALIDINPUT, // the input was invalid in some way e.g. null
        INPUT_NOT_AVAILABLE // not possible to get input data from the previous response
    }

    private STATUS status;
    private Object data;
    
    public EventDataObject(STATUS status, Object data)
    {
        super();
        this.status = status;
        this.data = data;
    }
    
    public STATUS getStatus()
    {
        return status;
    }
    
    public Object getData()
    {
        return data;
    }
}
