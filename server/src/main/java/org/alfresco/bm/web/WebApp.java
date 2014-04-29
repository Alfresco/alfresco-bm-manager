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
package org.alfresco.bm.web;

import java.io.File;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.alfresco.bm.test.TestConstants;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.GenericWebApplicationContext;

import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

/**
 * Initialize the web application
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class WebApp implements WebApplicationInitializer, TestConstants
{
    @Override
    public void onStartup(ServletContext container)
    {
        // Grab the server capabilities, otherwise just use the java version
        String javaVersion = System.getProperty("java.version");
        String systemCapabilities = System.getProperty(PROP_SYSTEM_CAPABILITIES, javaVersion);
        
        String appDir = new File(".").getAbsolutePath();
        String appContext = container.getContextPath();
        String appName = container.getContextPath().replace(SEPARATOR, "");
        
        // Create an application context (don't start, yet)
        @SuppressWarnings("resource")
        final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
                new String[]
                        {
                        "classpath:config/spring/app-context.xml"
                        },
                false);

        // Pass our properties to the new context
        Properties ctxProperties = new Properties();
        {
            ctxProperties.put(PROP_SYSTEM_CAPABILITIES, systemCapabilities);
            ctxProperties.put(PROP_APP_CONTEXT_PATH, appContext);
            ctxProperties.put(PROP_APP_DIR, appDir);
        }
        ConfigurableEnvironment ctxEnv = ctx.getEnvironment();
        ctxEnv.getPropertySources().addFirst(
                new PropertiesPropertySource(
                        appName,
                        ctxProperties));
        // Override all properties with system properties
        ctxEnv.getPropertySources().addFirst(
                new PropertiesPropertySource(
                        "system",
                        System.getProperties()));
        // Start the context now
        ctx.refresh();
        
        DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) ctx.getBeanFactory();
        
        GenericWebApplicationContext webCtx = new GenericWebApplicationContext(dlbf);
        ContextLoaderListener ctxLoaderListener = new ContextLoaderListener(webCtx);
        container.addListener(ctxLoaderListener);
        
        ServletRegistration.Dynamic jerseyServlet = container.addServlet("jersey-serlvet", SpringServlet.class);
        jerseyServlet.setInitParameter("com.sun.jersey.config.property.packages", "org.alfresco.bm.rest");
        jerseyServlet.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        jerseyServlet.addMapping("/api/*");
    }
}
