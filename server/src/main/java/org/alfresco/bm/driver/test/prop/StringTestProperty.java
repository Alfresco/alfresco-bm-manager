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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Support and validation of {@link TestPropertyType#STRING} test properties
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class StringTestProperty extends TestProperty
{
    public static final String PROP_MIN = "min";
    public static final String PROP_MAX = "max";
    public static final String PROP_HTML5 = "html5";
    public static final String PROP_REGEX = "regex";
    public static final String PROP_MASK = "mask";
    public static final String DEFAULT_VALUE = "";
    public static final int DEFAULT_MIN = 0;
    public static final int DEFAULT_MAX = 128;
    public static final String DEFAULT_REGEX = ".*";
    public static final boolean DEFAULT_MASK = false;

    public static Set<String> getValueNames()
    {
        Set<String> valueNames = TestProperty.getValueNames();
        valueNames.add(PROP_MIN);
        valueNames.add(PROP_MAX);
        valueNames.add(PROP_REGEX);
        valueNames.add(PROP_MASK);
        return valueNames;
    }
    
    private int min;
    private int max;
    private String regex;
    private boolean mask;
    @SuppressWarnings("unused")
    private Pattern pattern;
    
    /**
     * Build a 'string' property using the given values.
     */
    public StringTestProperty(String name, Properties properties)
    {
        super(name, properties);                    // Extracts common values
        
        // Extract minLength
        try
        {
            this.min = Integer.parseInt(properties.getProperty(PROP_MIN, "" + DEFAULT_MIN));
            if (this.min < 0)
            {
                throw new NumberFormatException("Minimum String length cannot be negative.");
            }
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
        // Extract regex
        try
        {
            regex = properties.getProperty(PROP_REGEX, DEFAULT_REGEX);
            pattern = Pattern.compile(regex);
        }
        catch (PatternSyntaxException e)
        {
            this.regex = DEFAULT_REGEX;
            this.pattern = Pattern.compile(regex);
            addError(name, PROP_REGEX, properties.getProperty(PROP_REGEX));
        }
        // Extract mask
        mask = Boolean.parseBoolean(properties.getProperty(PROP_MASK));
    }

   /**
     * @return          Returns {@link TestPropertyType#STRING} always
     */
    @Override
    public TestPropertyType getType()
    {
        return TestPropertyType.STRING;
    }
    
    @Override
    protected void addProperties(Properties properties)
    {
        properties.setProperty(PROP_MIN, "" + this.min);
        properties.setProperty(PROP_MAX, "" + this.max);
        properties.setProperty(PROP_REGEX, this.regex);
        properties.setProperty(PROP_MASK, "" + this.mask);
    }
    
    public int getMin()
    {
        return min;
    }
    
    public int getMax()
    {
        return max;
    }
    
    public String getRegex()
    {
        return regex;
    }

    public boolean isMask()
    {
        return mask;
    }
}
