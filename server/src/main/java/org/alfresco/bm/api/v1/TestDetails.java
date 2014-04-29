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
 * POJO representing test details
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class TestDetails
{
    private String name;
    private String oldName;
    private String copyOf;
    private int version;
    private String description;
    private String release;
    private Integer schema;

    public TestDetails()
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

    public void setOldName(String OldName)
    {
        this.oldName = OldName;
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

    public String getRelease()
    {
        return release;
    }

    public void setRelease(String release)
    {
        this.release = release;
    }

    public Integer getSchema()
    {
        return schema;
    }

    public void setSchema(Integer schema)
    {
        this.schema = schema;
    }
}
