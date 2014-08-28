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
package org.alfresco.bm.test;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.bm.test.prop.TestProperty;
import org.alfresco.bm.test.prop.TestPropertyFactory;

/**
 * Class that stores and is able to answer questions about the test configuration
 * i.e. provide defaults, etc.
 * <p/>
 * Every test needs one of these injected into the {@link Test main test instance}.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class TestDefaults implements LifecycleListener
{
    private final Properties rawProperties;
    private final String inheritance;
    
    private List<TestProperty> testProperties;
    private Map<String, TestProperty> testPropertiesMap;
    
    /**
     * @param description           the test description
     * @param rawProperties         raw Java properties describing the test properties in depth
     * 
     * @see TestPropertyFactory
     */
    public TestDefaults(Properties rawProperties, String inheritance)
    {
        if (rawProperties == null)
        {
            throw new IllegalArgumentException("rawProperties cannot be null");
        }
        if (inheritance == null || inheritance.length() == 0)
        {
            throw new IllegalArgumentException("inheritance must be supplied e.g. \"MYTEST,COMMON,HTTP\".");
        }
        this.rawProperties = rawProperties;
        this.inheritance = inheritance;
    }

    @Override
    public synchronized void start() throws Exception
    {
        testProperties = TestPropertyFactory.getTestProperties(inheritance, rawProperties);
        testPropertiesMap = TestPropertyFactory.groupByName(testProperties);
    }

    @Override
    public synchronized void stop() throws Exception
    {
    }

    public synchronized List<TestProperty> getPropertiesList()
    {
        if (testProperties == null)
        {
            throw new IllegalStateException("TestDefaults have not been started.");
        }
        return testProperties;
    }

    public synchronized Map<String, TestProperty> getPropertiesMap()
    {
        if (testPropertiesMap == null)
        {
            throw new IllegalStateException("TestDefaults have not been started.");
        }
        return testPropertiesMap;
    }
}
