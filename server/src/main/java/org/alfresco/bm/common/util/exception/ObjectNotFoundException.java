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
package org.alfresco.bm.common.util.exception;

/**
 * ObjectNotFoundException
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class ObjectNotFoundException extends Exception
{
    /**
     * Constructor
     * 
     * @param objectName
     *        (String, mandatory) Name of object that wasn't found although
     *        expected to be there.
     */
    public ObjectNotFoundException(String objectName)
    {
        super("Object '" + objectName + "' not found!");
    }

    /**
     * Constructor with a cause
     * 
     * @param objectName
     *        (String, mandatory) Name of object that wasn't found although
     *        expected to be there.
     * @param cause
     *        (Throwable) cause
     */
    public ObjectNotFoundException(String objectName, Throwable cause)
    {
        super("'" + objectName + "' not found!", cause);
    }

    /**
     * Checks and throws if object is null.
     * 
     * @param obj
     *        (Object) object expected not to be null
     * @param objName
     *        (String, mandatory), name of object
     * 
     * @throws ObjectNotFoundException
     */
    public static void checkObject(Object obj, String objName) throws ObjectNotFoundException
    {
        if (null == obj)
        {
            throw new ObjectNotFoundException(objName);
        }
    }
}
