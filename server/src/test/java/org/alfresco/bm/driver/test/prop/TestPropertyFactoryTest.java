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
package org.alfresco.bm.driver.test.prop;

import org.alfresco.bm.driver.test.TestDefaults;
import org.alfresco.bm.driver.test.prop.TestProperty.TestPropertyType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @see TestPropertyFactory
 * @see PropertiesTestPropertyFactory
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class TestPropertyFactoryTest
{
    private static final String INHERITANCE = "TEST, crud, sample, common";
    
    private ClassPathXmlApplicationContext ctx;
    private Properties rawProperties;

    @Before
    public void setUp()
    {
        // Initialize the app context; the following starts it, too.
        ctx = new ClassPathXmlApplicationContext("prop/test-TestPropertyFactory-context.xml");
        rawProperties = (Properties) ctx.getBean("testRawProperties");
    }
    
    @After
    public void tearDown()
    {
        ctx.close();
    }
    
    /**
     * Just ensure that the basic startup works
     */
    @Test
    public void basic()
    {
        List<TestProperty> listOne = TestPropertyFactory.getTestProperties(INHERITANCE, rawProperties);
        List<TestProperty> listTwo = TestPropertyFactory.getTestProperties(INHERITANCE, rawProperties);
        assertFalse("The factory does not produce singletons", listOne == listTwo);
    }
    
    @Test
    public void testDefaults() throws Exception
    {
        TestDefaults defaults = null;
        try
        {
            defaults = new TestDefaults(null, INHERITANCE);
            fail("Null properties not detected");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        defaults = new TestDefaults(rawProperties, INHERITANCE);
        // Don't initialize
        try
        {
            defaults.getPropertiesList();
            fail("Initialization should be done.");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }
        try
        {
            defaults.getPropertiesMap();
            fail("Initialization should be done.");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }
        defaults.start();
        defaults.getPropertiesList();
        defaults.getPropertiesMap();
    }
    
    @Test
    public void testTrap()
    {
        List<TestProperty> testProps = TestPropertyFactory.getTestProperties("trap", rawProperties);
        Map<String, TestProperty> mapProps = TestPropertyFactory.groupByName(testProps);
        assertEquals("Incorrect number of properties", 2, mapProps.size());
    }
    
    @Test
    public void testDefinitions()
    {
        List<TestProperty> testProps = TestPropertyFactory.getTestProperties(INHERITANCE, rawProperties);
        Map<String, TestProperty> mapProps = TestPropertyFactory.groupByName(testProps);
        assertEquals("Incorrect number of properties", 13, mapProps.size());
        
        TestProperty prop;
        StringTestProperty strProp;
        IntTestProperty intProp;
        DecimalTestProperty decProp;
        BooleanTestProperty booleanProp;
        
        prop = mapProps.get("one.str");
        strProp = (StringTestProperty) prop;
        {
            Properties strPropProperties = strProp.toProperties();
            strProp = new StringTestProperty("one.str", strPropProperties);
        }
        assertEquals(TestPropertyType.STRING, prop.getType());
        assertEquals("GROUP A", prop.getGroup());
        assertEquals("One Title (String)", prop.getTitle());
        assertEquals("One description (String)", prop.getDescription());
        assertEquals(false, prop.isHide());
        assertEquals("ONE DEFAULT", strProp.getDefault());
        assertEquals(0, strProp.getMin());
        assertEquals(256, strProp.getMax());
        assertEquals(".*", strProp.getRegex());
        assertEquals(true, strProp.isMask());
        assertEquals(0, strProp.getIndex());
        
        prop = mapProps.get("one.int");
        intProp = (IntTestProperty) prop;
        {
            Properties intPropProperties = intProp.toProperties();
            intProp = new IntTestProperty("one.int", intPropProperties);
        }
        assertEquals(TestPropertyType.INT, prop.getType());
        assertEquals("GROUP A", prop.getGroup());
        assertEquals("One Title (Integer)", prop.getTitle());
        assertEquals("One description (Integer)", prop.getDescription());
        assertEquals(false, prop.isHide());
        assertEquals("123", intProp.getDefault());
        assertEquals(0, intProp.getMin());
        assertEquals(256, intProp.getMax());
        assertEquals(0, strProp.getIndex());
        
        prop = mapProps.get("one.dec");
        decProp = (DecimalTestProperty) prop;
        {
            Properties decPropProperties = decProp.toProperties();
            decProp = new DecimalTestProperty("one.dec", decPropProperties);
        }
        assertEquals(TestPropertyType.DECIMAL, prop.getType());
        assertEquals("GROUP A", prop.getGroup());
        assertEquals("One Title (Decimal)", prop.getTitle());
        assertEquals("One description (Decimal)", prop.getDescription());
        assertEquals(false, prop.isHide());
        assertEquals("123.456", decProp.getDefault());
        assertEquals(-5.4, decProp.getMin(), 0.01);
        assertEquals(+5.4, decProp.getMax(), 0.01);
        assertEquals(0, strProp.getIndex());
        
        prop = mapProps.get("one.boolean");
        booleanProp = (BooleanTestProperty) prop;
        {
            Properties booleanPropProperties = booleanProp.toProperties();
            booleanProp = new BooleanTestProperty("one.boolean", booleanPropProperties);
        }
        assertEquals(TestPropertyType.BOOLEAN, prop.getType());
        assertEquals("GROUP A", prop.getGroup());
        assertEquals("One Title (Boolean)", prop.getTitle());
        assertEquals("One description (Boolean)", prop.getDescription());
        assertEquals(false, prop.isHide());
        assertEquals("true", booleanProp.getDefault());
        assertEquals(0, strProp.getIndex());
        
        // Test defaults i.e. where ONLY the default and type is provided
        
        prop = mapProps.get("two.str");
        strProp = (StringTestProperty) prop;
        assertEquals(TestPropertyType.STRING, prop.getType());
        assertEquals("two.str", prop.getName());
        assertEquals("", prop.getGroup());
        assertEquals("two.str", prop.getTitle());
        assertEquals("", prop.getDescription());
        assertEquals(false, prop.isHide());
        assertEquals("${one.str}", strProp.getDefault());
        assertEquals(0, strProp.getMin());
        assertEquals(128, strProp.getMax());
        assertEquals(".*", strProp.getRegex());
        assertEquals(false, strProp.isMask());
        assertEquals(0, strProp.getIndex());
        
        prop = mapProps.get("two.int");
        intProp = (IntTestProperty) prop;
        assertEquals(TestPropertyType.INT, prop.getType());
        assertEquals("two.int", prop.getName());
        assertEquals("", prop.getGroup());
        assertEquals("two.int", prop.getTitle());
        assertEquals("", prop.getDescription());
        assertEquals(false, prop.isHide());
        assertEquals("${one.int}", intProp.getDefault());
        assertEquals(0, intProp.getMin());
        assertEquals(Integer.MAX_VALUE, intProp.getMax());
        
        prop = mapProps.get("two.dec");
        decProp = (DecimalTestProperty) prop;
        assertEquals(TestPropertyType.DECIMAL, prop.getType());
        assertEquals("two.dec", prop.getName());
        assertEquals("", prop.getGroup());
        assertEquals("two.dec", prop.getTitle());
        assertEquals("", prop.getDescription());
        assertEquals(false, prop.isHide());
        assertEquals("${one.dec}", decProp.getDefault());
        assertEquals(0.0, decProp.getMin(), 0.01);
        assertEquals(Double.MAX_VALUE, decProp.getMax(), 0.01);
        
        prop = mapProps.get("two.boolean");
        booleanProp = (BooleanTestProperty) prop;
        assertEquals(TestPropertyType.BOOLEAN, prop.getType());
        assertEquals("two.boolean", prop.getName());
        assertEquals("", prop.getGroup());
        assertEquals("two.boolean", prop.getTitle());
        assertEquals("", prop.getDescription());
        assertEquals(false, prop.isHide());
        assertEquals("${one.boolean}", booleanProp.getDefault());
        
        // Test # Ignored if the '.default' value is not present
        assertEquals("No '.default' should mean no property", null, mapProps.get("three"));
        
        // Test inheritance and overriding
        
        prop = mapProps.get("four");
        intProp = (IntTestProperty) prop;
        assertEquals(TestPropertyType.INT, prop.getType());
        assertEquals("GROUP B", prop.getGroup());
        assertEquals("Four Title", prop.getTitle());
        assertEquals("Four description", prop.getDescription());
        assertEquals(true, prop.isHide());
        assertEquals("0", intProp.getDefault());
        assertEquals(0, intProp.getMin());
        assertEquals(65535, intProp.getMax());
        assertEquals(3, intProp.getIndex());
    }
}
