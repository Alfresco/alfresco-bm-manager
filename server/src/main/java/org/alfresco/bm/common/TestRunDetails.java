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
package org.alfresco.bm.common;

/**
 * POJO representing test run details
 * 
 * @author Derek Hulley
 * @author Frank Becker
 * 
 * @since 2.0
 */
public class TestRunDetails
{
    private String name;
    private String oldName;
    private String copyOf;
    private int version;
    private String description;
    private String benchmarkId;

    public TestRunDetails()
    {
    }

    /**
     * @return test name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the test name
     * 
     * @param name
     *        (String) test name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * gets the old test name (in case of copy)
     * 
     * @return (String) old test name
     */
    public String getOldName()
    {
        return oldName;
    }

    /**
     * sets the old test name (in case of copy)
     * 
     * @param oldName
     *        (String) old test name
     */
    public void setOldName(String oldName)
    {
        this.oldName = oldName;
    }

    /**
     * Gets if to create a copy of an old test
     */
    public String getCopyOf()
    {
        return copyOf;
    }

    /**
     * Sets if to create a copy of an old test
     */
    public void setCopyOf(String copyOf)
    {
        this.copyOf = copyOf;
    }

    /**
     * @return test version
     */
    public int getVersion()
    {
        return version;
    }

    /**
     * Sets the test version
     * 
     * @param version
     *        (int)
     */
    public void setVersion(int version)
    {
        this.version = version;
    }

    /**
     * Gets the test description
     * 
     * @return (String)
     */
    public String getDescription()
    {
        return description;
    }

    /** Sets the test description */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return Benchmark ID
     * @since 2.1.2
     */
    public String getBenchmarkId()
    {
        return benchmarkId;
    }

    /**
     * Sets Benchmark ID
     * 
     * @since 2.1.2
     */
    public void setBenchmarkId(String benchmarkId)
    {
        this.benchmarkId = benchmarkId;
    }
}
