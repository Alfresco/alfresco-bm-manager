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
package org.alfresco.bm.driver.test.prop;

import java.util.Properties;
import java.util.Set;

/**
 * Support and validation of {@link TestPropertyType#INT} test properties
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class IntTestProperty extends TestProperty
{
    public static final String PROP_MIN = "min";
    public static final String PROP_MAX = "max";
    public static final int DEFAULT_VALUE = 0;
    public static final int DEFAULT_MIN = 0;
    public static final int DEFAULT_MAX = Integer.MAX_VALUE;
    
    public static Set<String> getValueNames()
    {
        Set<String> valueNames = TestProperty.getValueNames();
        valueNames.add(PROP_MIN);
        valueNames.add(PROP_MAX);
        return valueNames;
    }
    
    private int min;
    private int max;
    
    /**
     * Build a 'string' property using the given values.
     */
    public IntTestProperty(String name, Properties properties)
    {
        super(name, properties);                    // Extracts common values
        
        // Extract min
        try
        {
            this.min = Integer.parseInt(properties.getProperty(PROP_MIN, "" + DEFAULT_MIN));
        }
        catch (NumberFormatException e)
        {
            this.min = DEFAULT_MIN;
            addError(name, PROP_MIN, properties.getProperty(PROP_MIN));
        }
        // Extract maxLength
        try
        {
            this.max = Integer.parseInt(properties.getProperty(PROP_MAX, "" + DEFAULT_MAX));
        }
        catch (NumberFormatException e)
        {
            this.max = DEFAULT_MAX;
            addError(name, PROP_MAX, properties.getProperty(PROP_MAX));
        }
    }

    /**
     * @return          Returns {@link TestPropertyType#INT} always
     */
    @Override
    public TestPropertyType getType()
    {
        return TestPropertyType.INT;
    }
    
    @Override
    protected void addProperties(Properties properties)
    {
        properties.setProperty(PROP_MIN, "" + this.min);
        properties.setProperty(PROP_MAX, "" + this.max);
    }

    public int getMin()
    {
        return min;
    }

    public int getMax()
    {
        return max;
    }
}
