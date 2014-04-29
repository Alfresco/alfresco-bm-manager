/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
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
package org.alfresco.bm.api.v1;

/**
 * POJO representing test run details
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class TestRunDetails
{
    private String name;
    private String oldName;
    private String copyOf;
    private int version;
    private String description;

    public TestRunDetails()
    {
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getOldName()
    {
        return oldName;
    }

    public void setOldName(String oldName)
    {
        this.oldName = oldName;
    }

    public String getCopyOf()
    {
        return copyOf;
    }

    public void setCopyOf(String copyOf)
    {
        this.copyOf = copyOf;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
