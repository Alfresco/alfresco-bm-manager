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
package org.alfresco.bm.test;

/**
 * Interface for classes that are able to estimate the progress of a test
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public interface CompletionEstimator
{
    /**
     * Get the latest test completion estimate
     * 
     * @return              a value from <b>0.0</b> (0%) to <b>1.0</b> (100%)
     */
    double getCompletion();

    /**
     * Shortcut method to determine if the test run has started or not
     * If it has not started, the {@link #getCompletion() completion} value will be <tt>0.0</tt>
     * 
     * @return              <tt>true</tt> if the test run has started
     */
    boolean isStarted();
    
    /**
     * Shortcut method to determine if the test run has started or not.
     * If it has completed, the {@link #getCompletion() completion} value will be <tt>1.0</tt>
     * 
     * @return              <tt>true</tt> if the test run has finished
     */
    boolean isCompleted();
    
    /**
     * @return              the number of successful results
     */
    long getResultsSuccess();
    
    /**
     * @return              the number of failed results
     */
    long getResultsFail();
}
