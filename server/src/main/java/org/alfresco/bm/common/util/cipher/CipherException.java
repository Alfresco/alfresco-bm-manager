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
package org.alfresco.bm.common.util.cipher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Cipher Exception class
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class CipherException extends Exception
{
    /** serial ID */
    private static final long serialVersionUID = -578031689334162353L;

    /**
     * Constructor
     * 
     * @param message
     *        Exception message
     * @param cause
     *        the cause
     */
    public CipherException(String message, Throwable cause)
    {
        super(message, cause);
        Log logger = LogFactory.getLog(this.getClass());
        logger.error(message, cause);
    }

    /**
     * Constructor
     * 
     * @param cause
     *        the cause
     */
    public CipherException(Throwable cause)
    {
        this("Genric cipher exception", cause);
        Log logger = LogFactory.getLog(this.getClass());
        logger.error("Cipher exception", cause);
    }

    /**
     * Constructor
     * 
     * @param message
     *        (String) exception message
     */
    public CipherException(String message)
    {
        super(message);
        Log logger = LogFactory.getLog(this.getClass());
        logger.error(message);
    }
}
