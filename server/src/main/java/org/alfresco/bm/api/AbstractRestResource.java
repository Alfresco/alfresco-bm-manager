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
package org.alfresco.bm.api;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.alfresco.bm.test.TestConstants;
import org.alfresco.bm.test.TestService.NotFoundException;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
public abstract class AbstractRestResource implements TestConstants
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
     * Generate a web exception for the given response status and message
     * 
     * @param status                the response status
     * @param msg                   a message to report
     * @throws WebApplicationException with the given status and wrapping the message and exception stack
     */
    protected void throwAndLogException(Status status, String msg)
    {
        // Only log an error if it's an internal server error
        switch (status)
        {
            case INTERNAL_SERVER_ERROR:
                logger.error(msg);
                break;
            default:
                logger.info(msg);
        }
        
        Map<String, String> jsonMap = new HashMap<String, String>(5);
        jsonMap.put("error", msg);
        
        String json = gson.toJson(jsonMap);
        
        Response response = Response.status(status).type(MediaType.APPLICATION_JSON).entity(json).build();
        throw new WebApplicationException(response);
    }
    
    /**
     * @throws WebApplicationException with the given status and wrapping the exception stack
     */
    protected void throwAndLogException(Status status, Exception e)
    {
        Throwable cause = ExceptionUtils.getRootCause(e);
        // Handle any well-known exceptions
        if (e instanceof NotFoundException || (cause != null && cause instanceof NotFoundException))
        {
            status = Status.NOT_FOUND;
        }
        // Only log locally if it's an internal server error
        switch (status)
        {
            case INTERNAL_SERVER_ERROR:
                logger.error(e);
                break;
            default:
                logger.info(e);
        }
        
        throw new WebApplicationException(e, status);
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
     * @param obj           the object to modify
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
