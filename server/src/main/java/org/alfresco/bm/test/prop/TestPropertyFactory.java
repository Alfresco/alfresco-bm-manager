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
package org.alfresco.bm.test.prop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.bm.test.prop.TestProperty.TestPropertyType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factory bean for {@link TestProperty test property definitions}.
 * <p/>
 * This factory picks up all loaded properties passed to it and uses a naming convention
 * to isolate test property defaults and related values.
 * <p/>
 * So, if a property <b>abc</b> is required by the test, then the following needs to be
 * put into a properties file; the search for additional properties files is
 * <b>classpath:config/defaults/*.properties</b>:
 * <ul>
 *  <li>${test.projectName}.abc.default=Some default</li>
 *  <li>${test.projectName}.abc.group=Group name: properties can be logically ordered by into groups for display purposes</li>
 *  <li>${test.projectName}.abc.title=Label or title</li>
 *  <li>${test.projectName}.abc.description=Help text</li>
 *  <li>${test.projectName}.abc.type=int/string/decimal/boolean</li>
 *  <li>${test.projectName}.abc.min=Minimum value or minimum length (int/string/decimal types only)</li>
 *  <li>${test.projectName}.abc.max=Maximum value or maximum length (int/string/decimal types only)</li>
 *  <li>${test.projectName}.abc.regex=A regular expression to control string validation (string type only) (default:'.*')</li>
 *  <li>${test.projectName}.abc.mask=true to hide the value when test runs values are recorded e.g. password=***** (string type only) (default:false)</li>
 *  <li>${test.projectName}.abc.hide=true if the property is for internal use (default:false)</li>
 * </ul>
 * Inheritance order is strictly defined using property:<br/>
 * <b>inheritance=[projectA].[projectB].[etc]</b><br/>
 * The default inheritance order is:<br/>
 * <b>inheritance=common<b/>
 * i.e. the common project's values are retrieved first and then overlayed with values from other projects.
 * The property should be defined for all tests where values for specific properties need to be modified.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public abstract class TestPropertyFactory
{
    /** Property names must start with a letter and then may only contain letters, numbers or dash (-) */
    public static final String PROP_NAME_REGEX = "^[a-zA-Z]+[\\-\\.a-zA-Z0-9]*";
    private static final Pattern PROP_NAME_PATTERN = Pattern.compile(PROP_NAME_REGEX);
    public static final String PROP_INHERITANCE = "inheritance";
    public static final String DEFAULT_INHERITANCE = "common";
    public static final String SUFFIX_DEFAULT = ".default";

    private static Log logger = LogFactory.getLog(TestPropertyFactory.class);
    
    /**
     * Helper method to create a map of test properties keyed by their names
     */
    public static Map<String, TestProperty> groupByName(List<TestProperty> testProperties)
    {
        Map<String, TestProperty> map = new HashMap<String, TestProperty>(123);
        for (TestProperty testProperty : testProperties)
        {
            map.put(testProperty.getName(), testProperty);
        }
        return map;
    }
    
    /**
     * Convert Java properties into instances of {@link TestProperty} instances.
     * 
     * @param properties            Java properties with values defining defaults, types, etc
     * @return                      objects mapping all the values required to persist and present the test properties
     */
    public static List<TestProperty> getTestProperties(Properties properties)
    {
        // Extract project and property names
        Set<String> allPropNames = new HashSet<String>(113);
        Set<String> allProjectNames = new HashSet<String>(5);
        getPropertyAndProjectNames(properties, allPropNames, allProjectNames);
        
        // Get property inheritance order
        TreeSet<String> inheritance = getInheritance(properties);
        // Append all other projects and warn appropriately
        if (inheritance.addAll(allProjectNames))
        {
            logger.warn("The project property inheritance has not been explicitly defined.  Projects are: " + inheritance);
        }
        
        // Build the properties
        List<TestProperty> testProperties = new ArrayList<TestProperty>(57);
        for (String propName : allPropNames)
        {
            // Eliminate non-compliat properties
            Matcher propNameMatcher = PROP_NAME_PATTERN.matcher(propName);
            if (!propNameMatcher.matches())
            {
                logger.error("Ignoring illegal property name '" + propName + "'.  Values must match '" + PROP_NAME_REGEX + "' " +
                             " i.e. start with a letter and then include letters, numbers, dots or dashes.");
                continue;
            }
            
            TestProperty testProperty = getProperty(properties, propName, inheritance);
            testProperties.add(testProperty);
        }
        // Done
        return testProperties;
    }
    
    /**
     * Get an ordered set of project inheritance
     */
    private static TreeSet<String> getInheritance(Properties properties)
    {
        TreeSet<String> projects = new TreeSet<String>();
        
        String inheritance = properties.getProperty(PROP_INHERITANCE, DEFAULT_INHERITANCE);
        StringTokenizer st = new StringTokenizer(inheritance, ".");
        while (st.hasMoreTokens())
        {
            String project = st.nextToken();
            if (project.length() == 0)
            {
                continue;
            }
            projects.add(project);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Project inheritance: " + projects);
        }
        return projects;
    }
    
    private static void getPropertyAndProjectNames(Properties properties, Set<String> propNames, Set<String> projectNames)
    {
        for (Object propDeclarationObj : properties.keySet())
        {
            String propDeclaration = (String) propDeclarationObj;
            // Ignore everything that is not a "*.default" property, which marks a true property definition
            if (!propDeclaration.endsWith(SUFFIX_DEFAULT))
            {
                continue;
            }
            // Find end of project name
            int projectEndDot = propDeclaration.indexOf('.');
            if (projectEndDot < 1)
            {
                logger.debug("Ignoring property: " + propDeclaration);
                continue;
            }
            int propDeclarationLen = propDeclaration.length();
            int propEndDot = propDeclarationLen - SUFFIX_DEFAULT.length();
            // Ignore if there is no substance between the first dot and default suffix e.g. project..default=
            int propNameStart = projectEndDot + 1;
            int propNameLen = (propEndDot - projectEndDot) -1;          // Length of characters between dots
            if (projectEndDot < 0 || propNameLen  < 1)
            {
                logger.debug("Ignoring property: " + propDeclaration);
                continue;
            }
            String projectName = propDeclaration.substring(0, projectEndDot);
            String propName = propDeclaration.substring(propNameStart, propEndDot);
            // Add it
            projectNames.add(projectName);
            propNames.add(propName);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("All projects: " + projectNames);
            logger.debug("All properties: " + propNames);
        }
    }
    
    /**
     * Do the actual work of building the java option
     */
    private static TestProperty getProperty(Properties properties, String property, TreeSet<String> projects)
    {
        // Attempt to determine the type
        TestPropertyType type = null;
        for (String project : projects)
        {
            String typeStr = getValue(properties, project, property, TestProperty.PROP_TYPE);
            // Ignore a type that is not set or differs
            try
            {
                type = TestPropertyType.valueOf(typeStr == null ? null : typeStr.toUpperCase().trim());
                // Successfully got the type
            }
            catch (Exception e)
            {
            }
        }
        // Check that some type was given
        if (type == null)
        {
            type = TestPropertyType.STRING;
            logger.warn("Assuming " + TestPropertyType.STRING + " as type for " + property);
        }
        // We have a type.  Determine the value names to get
        Set<String> valueNames = type.getValueNames();
        
        // Get all the values in project override order
        Properties testPropertyValues = new Properties();
        for (String project : projects)
        {
            for (String valueName : valueNames)
            {
                String value = getValue(properties, project, property, valueName);
                if (value == null)
                {
                    // Not supplied
                    continue;
                }
                testPropertyValues.put(valueName, value);
            }
        }
        // We have now overwritten (repeatedly) the values
        // Construct it
        TestProperty testProperty = type.createTestProperty(property, testPropertyValues);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Built '" + property + "' from projects " + projects + ": " + testProperty);
        }
        return testProperty;
    }
    
    /**
     * For a given project, property name and value name, retrieve the property value.<br/>
     * Format is: <b>project.property.valueName=value</b><br/>
     * 
     * @param properties        the source of all required data for operation
     * @param valueName         the property value name e.g. <b>type<b>
     * @return                  the property value or null
     */
    private static String getValue(Properties properties, String project, String property, String valueName)
    {
        String key = project + "." + property + "." + valueName;
        return properties.getProperty(key);
    }
}
