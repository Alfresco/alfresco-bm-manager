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
