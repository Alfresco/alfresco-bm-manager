/*
 * #%L
 * Alfresco Benchmark Framework Manager
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
package org.alfresco.bm.publicapi.factory;

/**
 * Site exception
 * 
 * @author Frank Becker
 * 
 * @since 2.1.2
 */
public class SiteException extends Exception
{
    /** Serialization ID */
    private static final long serialVersionUID = 5858602498644137295L;

    /**
     * Constructor
     * 
     * @param cause
     *        cause of exception
     */
    public SiteException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructor
     * 
     * @param message
     *        Exception message
     */
    public SiteException(String message)
    {
        super(message);
    }

    /**
     * Constructor
     * 
     * @param msg
     *        Exception message
     * @param cause
     *        exception cause
     */
    public SiteException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
