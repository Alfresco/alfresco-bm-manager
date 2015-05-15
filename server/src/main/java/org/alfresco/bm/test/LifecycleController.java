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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

/**
 * Context-aware bean that initializes the application
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class LifecycleController
        implements ApplicationListener<ApplicationContextEvent>, ApplicationContextAware, BeanNameAware
{
    private static Log logger = LogFactory.getLog(LifecycleController.class);

    private ApplicationContext ctx;
    private String name;
    private boolean started = false;
    private final List<LifecycleListener> lifecycleListeners;
    private boolean forceStart;
    
    /** Record any messages for later reporting */
    private final StringBuilder log = new StringBuilder(1024);
    
    /**
     * @param lifecycleListeners                listeners that will be notified of app state changes
     */
    public LifecycleController(LifecycleListener ... lifecycleListeners)
    {
        this.lifecycleListeners = new ArrayList<LifecycleListener>(5);
        for (LifecycleListener lifecycleListener : lifecycleListeners)
        {
            this.lifecycleListeners.add(lifecycleListener);
        }
        this.forceStart = false;
    }
    
    /**
     * Change how startup failures are handled
     * 
     * @param forceStart                <tt>true</tt> to report exceptions only or <tt>false</tt> to report and rethrow
     */
    public void setForceStart(boolean forceStart)
    {
        this.forceStart = forceStart;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException
    {
        this.ctx = ctx;
    }

    @Override
    public void setBeanName(String beanName)
    {
        this.name = beanName;
    }

    @Override
    public void onApplicationEvent(ApplicationContextEvent event)
    {
        // Ignore events from different application contexts
        if (event.getApplicationContext() != ctx)
        {
            // Ignore
            return;
        }
        
        String eventName = event.getClass().getSimpleName();
        if (eventName.equals("ContextRefreshedEvent"))
        {
            // Only start once
            if (!started)
            {
                start();
                started = true;
            }
        }
        else if (eventName.equals("ContextClosedEvent"))
        {
            // Only stop once
            if (started)
            {
                stop();
                started = false;
            }
        }
    }
    
    /**
     * Start all listeners
     */
    private synchronized void start()
    {
        if (started)
        {
            // Someone jumped in ahead of us
            return;
        }
        // We start no matter what
        started = true;
        
        StringBuffer sb = new StringBuffer(1024);
        sb.append("\nStarting ...");
        
        if (logger.isDebugEnabled())
        {        
            logger.debug("Starting components: " + name);
        }
        for (LifecycleListener listener : lifecycleListeners)
        {
            String listenerName = listener.getClass().getName();
            if (logger.isDebugEnabled())
            {
                logger.debug("   Starting component: " + listenerName);
            }
            try
            {
                listener.start();
                if (logger.isDebugEnabled())
                {
                    logger.debug("   Started component: " + listenerName);
                }
                sb.append("\n   Started component: " + listenerName);
            }
            catch (Exception e)
            {
                // Absorb the exception
                logger.error("   Failed to start component: " + listenerName, e);
                sb.append("\n   Failed to start component: " + listenerName);
                StringWriter stackWriter = new StringWriter(1024);
                WriterOutputStream wos = new WriterOutputStream(stackWriter);
                PrintWriter pw = new PrintWriter(wos);
                try
                {
                    e.printStackTrace(pw);
                }
                finally
                {
                    try { pw.close(); } catch (Exception ee) {}
                }
                sb.append("\n").append(stackWriter.getBuffer().toString());
                // Now respect the forceStart option
                if (!forceStart)
                {
                    throw new RuntimeException("Component failed to start: " + listenerName, e);
                }
            }
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Started components: " + name);
        }
        log.append(sb.toString());
    }
    
    /**
     * Stop all listeners
     */
    private synchronized void stop()
    {
        if (!started)
        {
            // Someone jumped in ahead of us
            return;
        }
        // We have stopped no matter what
        started = false;
        
        StringBuffer sb = new StringBuffer(1024);
        sb.append("\nStopping ...");
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Stopping components: " + name);
        }
        for (LifecycleListener listener : lifecycleListeners)
        {
            String listenerName = listener.getClass().getName();
            if (logger.isDebugEnabled())
            {
                logger.debug("   Stopping component: " + listenerName);
            }
            try
            {
                listener.stop();
                if (logger.isDebugEnabled())
                {
                    logger.debug("   Stopped component: " + listenerName);
                }
                sb.append("\n   Stopped component: " + listenerName);
            }
            catch (Exception e)
            {
                // Absorb the exception
                logger.error("   Failed to stop component: " + listenerName, e);
                sb.append("\n   Failed to stop component: " + listenerName);
                StringWriter stackWriter = new StringWriter(1024);
                WriterOutputStream wos = new WriterOutputStream(stackWriter);
                PrintWriter pw = new PrintWriter(wos);
                try
                {
                    e.printStackTrace(pw);
                }
                finally
                {
                    try { pw.close(); } catch (Exception ee) {}
                }
                sb.append("\n").append(stackWriter.getBuffer().toString());
            }
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Stopped components: " + name);
        }
        log.append(sb.toString());
    }
    
    /**
     * Get any log messages generated during startup or shutdown
     * 
     * @return                          log messages
     */
    public String getLog()
    {
        return log.toString();
    }
}
