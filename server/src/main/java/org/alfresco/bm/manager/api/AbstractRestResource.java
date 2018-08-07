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
package org.alfresco.bm.manager.api;

import static org.alfresco.bm.common.TestConstants.FIELD_DEFAULT;
import static org.alfresco.bm.common.TestConstants.FIELD_MASK;
import static org.alfresco.bm.common.TestConstants.FIELD_PROPERTIES;
import static org.alfresco.bm.common.TestConstants.FIELD_VALUE;
import static org.alfresco.bm.common.TestConstants.MASK;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

/**
 * Support for REST resources.
 * 
 * @author Michael Suzuki
 * @author Derek Hulley
 * @since 2.0
 */
public abstract class AbstractRestResource
{
    protected final Log logger;
    protected final Gson gson;

    /**
     * @since 2.0
     */
    protected AbstractRestResource()
    {
        logger = LogFactory.getLog(this.getClass());
        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();
    }

    /**
     * Does a deep copy of an object to allow for subsequent modification
     */
    public static DBObject copyDBObject(DBObject dbObject)
    {
        DBObject orig = dbObject;
        BasicDBObjectBuilder dbObjectBuilder = BasicDBObjectBuilder.start();
        for (String field : orig.keySet())
        {
            Object value = orig.get(field);
            dbObjectBuilder.add(field, value);
        }
        return dbObjectBuilder.get();
    }

    /**
     * Find and mask property values.
     * <p/>
     * Properties will be searched for deeply.
     * 
     * @param dbObject
     *            the object to modify
     */
    public static DBObject maskValues(DBObject dbObject)
    {
        if (dbObject instanceof BasicDBList)
        {
            BasicDBList objListOrig = (BasicDBList) dbObject;
            // Copy entries to a new list
            BasicDBList newObjList = new BasicDBList();
            for (Object origListObjT : objListOrig)
            {
                DBObject origListObj = (DBObject) origListObjT;
                // Mask any values
                DBObject newListObj = maskValues(origListObj);
                newObjList.add(newListObj);
            }
            // Done
            return newObjList;
        }
        else if (dbObject.containsField(FIELD_MASK))
        {
            boolean mask = Boolean.parseBoolean((String) dbObject.get(FIELD_MASK));
            if (mask)
            {
                DBObject newObj = copyDBObject(dbObject);
                // We have a copy to play with
                newObj.put(FIELD_DEFAULT, MASK);
                if (dbObject.get(FIELD_VALUE) != null)
                {
                    newObj.put(FIELD_VALUE, MASK);
                }
                return newObj;
            }
            else
            {
                return dbObject;
            }
        }
        else if (dbObject.containsField(FIELD_PROPERTIES))
        {
            // There are properties
            BasicDBList propsObj = (BasicDBList) dbObject.get(FIELD_PROPERTIES);
            BasicDBList newPropsObj = (BasicDBList) maskValues(propsObj);
            // Copy
            DBObject newObj = copyDBObject(dbObject);
            newObj.put(FIELD_PROPERTIES, newPropsObj);
            // Done
            return newObj;
        }
        else
        {
            // Not a list and does not contain the mask field
            return dbObject;
        }
    }
}
