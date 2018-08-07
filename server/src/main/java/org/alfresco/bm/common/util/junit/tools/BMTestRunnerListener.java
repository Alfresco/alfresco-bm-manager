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
package org.alfresco.bm.common.util.junit.tools;

import org.springframework.context.ApplicationContext;

/**
 * Interface that receives callbacks from the {@link BMTestRunner}.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public interface BMTestRunnerListener
{
    /**
     * Notification that the test context has started.
     */
    void testReady(ApplicationContext testCtx, String test);
    
    /**
     * Notification that the test run has been created but not started.
     */
    void testRunReady(ApplicationContext testCtx, String test, String run);
    
    /**
     * Notification that the test run started.
     */
    void testRunStarted(ApplicationContext testCtx, String test, String run);
    
    /**
     * Notification that the test run finished successfully
     */
    void testRunFinished(ApplicationContext testCtx, String test, String run);
}
