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
package org.alfresco.bm.test.prop;

import java.util.Properties;
import java.util.Set;

/**
 * Support and validation of {@link TestPropertyType#BOOLEAN} test properties
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class BooleanTestProperty extends TestProperty
{
    public static final boolean DEFAULT_VALUE = true;
    
    public static Set<String> getValueNames()
    {
        Set<String> valueNames = TestProperty.getValueNames();
        return valueNames;
    }
    
    /**
     * Build a 'string' property using the given values.
     */
    public BooleanTestProperty(String name, Properties properties)
    {
        super(name, properties);                    // Extracts common values
    }

    /**
     * @return          Returns {@link TestPropertyType#BOOLEAN} always
     */
    @Override
    public TestPropertyType getType()
    {
        return TestPropertyType.BOOLEAN;
    }
    
    @Override
    protected void addProperties(Properties properties)
    {
    }
}
