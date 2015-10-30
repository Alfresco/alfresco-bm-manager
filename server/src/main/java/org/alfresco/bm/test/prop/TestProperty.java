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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class to hold test properties and provide support for validation
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public abstract class TestProperty implements Comparable<TestProperty>
{
    public static final String PROP_NAME = "name";
    public static final String PROP_TYPE = "type";
    public static final String PROP_DEFAULT = "default";
    public static final String PROP_GROUP = "group";
    public static final String PROP_TITLE = "title";
    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_HIDE = "hide";
    public static final String PROP_MASK = "mask";
    
    /** @since 2.1 - optional validation for the property */
    public static final String PROP_VALIDATION = "validation";
    
    /** @since 2.1 - optional choice collection for validation */
    public static final String PROP_CHOICE_COLLECTION = "choices";
    
    /** @since 2.1 - optional dependencies for a property */
    public static final String PROP_DEPENDENCIES = "dependencies";
    
    /**
     * Enumeration of the basic property types supported
     * 
     * @author Derek Hulley
     * @since 2.0
     */
    public static enum TestPropertyType
    {
        STRING
        {
            @Override
            public Set<String> getValueNames()
            {
                return StringTestProperty.getValueNames();
            }
            @Override
            public TestProperty createTestProperty(String name, Properties properties)
            {
                return new StringTestProperty(name, properties);
            }
        },
        BOOLEAN
        {
            @Override
            public Set<String> getValueNames()
            {
                return BooleanTestProperty.getValueNames();
            }
            @Override
            public TestProperty createTestProperty(String name, Properties properties)
            {
                return new BooleanTestProperty(name, properties);
            }
        },
        INT
        {
            @Override
            public Set<String> getValueNames()
            {
                return IntTestProperty.getValueNames();
            }
            @Override
            public TestProperty createTestProperty(String name, Properties properties)
            {
                return new IntTestProperty(name, properties);
            }
        },
        DECIMAL
        {
            @Override
            public Set<String> getValueNames()
            {
                return DecimalTestProperty.getValueNames();
            }
            @Override
            public TestProperty createTestProperty(String name, Properties properties)
            {
                return new DecimalTestProperty(name, properties);
            }
        },
        ;
        public abstract Set<String> getValueNames();
        public abstract TestProperty createTestProperty(String name, Properties properties);
    }
    
    /**
     * Fetch the values that should be defined for a test property.
     * Override to add more values depending on the specialization.
     * 
     * @return          the set of values that constitute a definition.
     */
    public static Set<String> getValueNames()
    {
        Set<String> valueNames = new HashSet<String>(13);
        valueNames.add(PROP_NAME);
        valueNames.add(PROP_TYPE);
        valueNames.add(PROP_DEFAULT);
        valueNames.add(PROP_GROUP);
        valueNames.add(PROP_TITLE);
        valueNames.add(PROP_DESCRIPTION);
        valueNames.add(PROP_HIDE);
        valueNames.add(PROP_MASK);
        // TODO - this values are optional - make sure that IF these values are still required if the configured values are empty!
        valueNames.add(PROP_VALIDATION);
        valueNames.add(PROP_CHOICE_COLLECTION);
        valueNames.add(PROP_DEPENDENCIES);
        return valueNames;
    }
    
    private final Log logger = LogFactory.getLog(this.getClass());
    private final List<String> errors;
    private final String name;
    private final String defaultValue;
    private final String group;
    private final String title;
    private final String description;
    private final boolean hide;
    private final boolean mask;
    private final String validation;
    private final String choices;
    private final String dependencies;
    
    /**
     * Construct a test property
     */
    public TestProperty(String name, Properties properties)
    {
        this.errors = new ArrayList<String>(0);
        
        this.name = name;
        // Extract common values
        this.defaultValue = properties.getProperty(PROP_DEFAULT, "");
        this.title = properties.getProperty(PROP_TITLE, name);
        this.group = properties.getProperty(PROP_GROUP, "");
        this.description = properties.getProperty(PROP_DESCRIPTION, "");
        this.hide = Boolean.parseBoolean(properties.getProperty(PROP_HIDE, "false"));
        this.mask = Boolean.parseBoolean(properties.getProperty(PROP_MASK, "false"));

        // since 2.1 (optional extra values if to specify a "special" validation or a list of allowed values) 
        this.validation =  properties.getProperty(PROP_VALIDATION, "");
        this.choices = properties.getProperty(PROP_CHOICE_COLLECTION, "");
        this.dependencies = properties.getProperty(PROP_DEPENDENCIES, "");
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TestProperty ")
          .append("[type=").append(getType())
          .append(", name=").append(name)
          .append(", default=").append(defaultValue)
          .append(", group=").append(group)
          .append(", title=").append(title)
          .append(", description=").append(description)
          .append(", hide=").append(hide)
          .append(", mask=").append(mask);
      // since 2.1 append optional values 
      if (!this.validation.isEmpty())
      {
          sb.append(", validation=").append(this.validation);
      }
      if (!this.choices.isEmpty())
      {
          sb.append(", choices=").append(this.choices);
      }
      if (!this.dependencies.isEmpty())
      {
          sb.append(", dependencies=").append(this.dependencies);
      }
      sb.append("]");
      return sb.toString();
    }

    /**
     * Hashcode is based purely on <b>name</b>.
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * Equality is based purely on <b>name</b>.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        TestProperty other = (TestProperty) obj;
        if (name == null)
        {
            if (other.name != null) return false;
        }
        else if (!name.equals(other.name)) return false;
        return true;
    }

    /**
     * Compares based on <b>group</b> and <b>name</b>.
     */
    @Override
    public int compareTo(TestProperty o)
    {
        // Group ordering is first
        int groupCompare = this.group.compareTo(o.group);
        if (groupCompare != 0)
        {
            return groupCompare;
        }
        // Same group, so just compare the name
        return this.name.compareTo(o.name);
    }

    /**
     * Record and error condition against the test property.  Property definitions must not
     * fail with an exception condition but must, instead, record all errors for later.
     */
    public void addError(String error)
    {
        errors.add(error);
    }
    
    /**
     * Record an error when converting a value
     * 
     * @param name                  the name of the property
     * @param valueName             the value name e.g. 'minLength'
     * @param value                 the actual value supplied e.g. 'A'
     */
    public void addError(String name, String valueName, String value)
    {
        addError("Invalid '" + valueName + "' given for property '" + name + "': " + value);
    }
    
    /**
     * @return              the type of the instance
     */
    public abstract TestPropertyType getType();
    
    /**
     * Convert the instance back into raw Java properties
     */
    public Properties toProperties()
    {
        Properties properties = new Properties();
        properties.setProperty(PROP_NAME, name);
        properties.setProperty(PROP_TYPE, getType().toString());
        properties.setProperty(PROP_DEFAULT, defaultValue);
        properties.setProperty(PROP_GROUP, group);
        properties.setProperty(PROP_TITLE, title);
        properties.setProperty(PROP_DESCRIPTION, description);
        properties.setProperty(PROP_HIDE, "" + hide);
        properties.setProperty(PROP_MASK, "" + mask);
        
        // add optional values (since 2.1)
        if (!this.validation.isEmpty())
        {
            properties.setProperty(PROP_VALIDATION, this.validation);
        }
        if (!this.choices.isEmpty())
        {
            properties.setProperty(PROP_CHOICE_COLLECTION, this.choices);
        }
        if (!this.dependencies.isEmpty())
        {
            properties.setProperty(PROP_DEPENDENCIES, this.dependencies);
        }
        // Add derived type properties
        addProperties(properties);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Generated Java properties: " + properties);
        }
        return properties;
    }

    /**
     * Derived instances must override this and attach additional, type-specific properties
     */
    protected abstract void addProperties(Properties properties);
    
    public List<String> getErrors()
    {
        return errors;
    }

    public String getName()
    {
        return name;
    }

    public String getDefault()
    {
        return defaultValue;
    }

    public String getGroup()
    {
        return group;
    }

    public String getTitle()
    {
        return title;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isHide()
    {
        return hide;
    }

    public boolean isMask()
    {
        return mask;
    }
    
    /**
     * @return validation name for UI - the JavaScript validation called for this property
     * Note: this value is optional and defaults to "type" in the JavaScript code. This 
     * means validation will be done on the type (boolean, int, decimal or string) and the 
     * up-to 2.0.10 defined properties only. But you may specify a special validation for 
     * example to make sure an URL is reachable.
     * @since 2.1 
     */
    public String getValidation()
    {
        return this.validation;
    }
    
    /**
     * @return (String) choice collection. This a a JSON string containing the array values that 
     * are allowed as values for the property.value. 
     * Note: this value is optional. If configured, only the values in the array are allowed values 
     * for the property.value and property.defaultValue. 
     * @since 2.1
     */
    public String getChoices()
    {
        return this.choices;
    }
    
    /**
     * @return (String) dependencies. This is a JSON string containing the group and names of one or more properties 
     * this property depends on. Whenever a depended property changes it value (or defaultValue) the validation
     * of this property will also be executed. This is useful for example for URL properties that contain 
     * the server name as a placeholder / variable. If for example the host name contained in the URL 
     * is updated, the URL will be validated. If specified a validation this can also trigger a 
     * round-trip to the server to make sure the URL is reachable. 
     */
    public String getDependencies()
    {
        return this.dependencies;
    }
}
