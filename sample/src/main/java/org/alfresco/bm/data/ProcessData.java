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
package org.alfresco.bm.data;

/**
 * Sample POJO for demonstration.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class ProcessData
{
    public static final String FIELD_NAME = "name";
    public static final String FIELD_STATE = "state";

    private String name;
    private DataCreationState state;
    
    public ProcessData()
    {
        state = DataCreationState.Unknown;
    }

    public String getName()
    {
        return name;
    }
    public void setName(String processName)
    {
        this.name = processName;
    }
    public DataCreationState getState()
    {
        return state;
    }
    public void setState(DataCreationState state)
    {
        this.state = state;
    }
}
