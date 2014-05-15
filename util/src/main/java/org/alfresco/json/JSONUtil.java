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
package org.alfresco.json;

import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Utilities for JSON-parsing.
 * 
 * @author Frederik Heremans
 * @author Michael Suzuki
 */
public class JSONUtil
{
    // General constants

    public static final String JSON_DATA = "data";
    public static final String JSON_ID = "id";
    public static final String JSON_ITEMS = "items";
    public static final String JSON_NAME = "name";
    public static final String FORM_PROCESSOR_JSON_PERSISTED_OBJECT = "persistedObject";
    public static final String MIME_TYPE_JSON = "application/json";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String UTF_8_ENCODING = "UTF-8";

    private static Log logger = LogFactory.getLog(JSONUtil.class);

    /**
     * Sets a header to notify server we expect a JSON-repsponse for this method.
     */
    public static void setJSONExpected(HttpPost method)
    {
        method.setHeader(HEADER_ACCEPT, MIME_TYPE_JSON);
    }

    /**
     * Adds the JSON as request-body the the method and sets the correct content-type.
     */
    public static void populateRequestBody(HttpPost method, JSONObject object)
    {
        try
        {
            method.setEntity(setMessageBody(object));
        }
        catch (UnsupportedEncodingException error)
        {
            // This will never happen!
            throw new RuntimeException("UTF-8 encoding not supported by native system");
        }
    }

    /**
     * Extract the "data" JSON-object from the method's response.
     * 
     * @param method the method containing the response
     * @return the "data" object. Returns null if response is not JSON or no data-object is present.
     */
    public static JSONObject getDataFromResponse(HttpEntity method)
    {
        JSONObject result = null;
        JSONObject response;
        try
        {
            response = readStream(method);
            // Extract object for "data" property
            result = (JSONObject) response.get(JSONUtil.JSON_DATA);
        }
        catch (Exception e)
        {
            logger.error("Unable to parse response to json");
        }
        return result;
    }

    /**
     * Extract the "data" JSON-array from the method's response.
     * 
     * @param method the method containing the response
     * @return the "data" object. Returns null if response is not JSON or no data-object is present.
     */
    public static JSONArray getDataArrayFromResponse(HttpEntity method)
    {
        JSONArray result = null;
        JSONObject json = readStream(method);
        if (json != null)
        {
            // Extract object for "data" property
            Object jsonData = (Object) json.get(JSONUtil.JSON_DATA);
            if (jsonData instanceof JSONArray)
            {
                result = (JSONArray) jsonData;
            }
            else
            {
                throw new ClassCastException("Unable to turn JSON object into JSONArray: " + jsonData);
            }
        }
        return result;
    }

    /**
     * Gets a string-value from the given JSON-object for the given key.
     * 
     * @param json the json object
     * @param key key pointing to the value
     * @param defaultValue if value is not set or if value is not of type "String", this value is returned
     */
    public static String getString(JSONObject json, String key, String defaultValue)
    {
        String result = defaultValue;

        if (json != null)
        {
            Object value = json.get(key);
            if (value instanceof String)
            {
                result = (String) value;
            }
        }
        return result;
    }

    /**
     * @param json JSON to extract array from
     * @param key key under which array is present on JSON
     * @return the {@link JSONArray}. Returns null, if the value is null or not an array.
     */
    public static JSONArray getArray(JSONObject json, String key)
    {
        Object object = json.get(key);
        if (object instanceof JSONArray) { return (JSONArray) object; }
        return null;
    }

    /**
     * @param json JSON to extract object from
     * @param key key under which object is present on JSON
     * @return the {@link JSONObject}. Returns null, if the value is null or not an object.
     */
    public static JSONObject getObject(JSONObject json, String key)
    {
        Object object = json.get(key);
        if (object instanceof JSONObject) { return (JSONObject) object; }
        return null;
    }

    /**
     * Populate HTTP message call with given content.
     * 
     * @param content String content
     * @return {@link StringEntity} content.
     * @throws UnsupportedEncodingException if unsupported
     */
    public static StringEntity setMessageBody(final String content) throws UnsupportedEncodingException
    {
        if (content == null || content.isEmpty()) throw new UnsupportedOperationException("Content is required.");
        return new StringEntity(content, UTF_8_ENCODING);
    }

    /**
     * Populate HTTP message call with given content.
     * 
     * @param json {@link JSONObject} content
     * @return {@link StringEntity} content.
     * @throws UnsupportedEncodingException if unsupported
     */
    public static StringEntity setMessageBody(final JSONObject json) throws UnsupportedEncodingException
    {
        if (json == null || json.toString().isEmpty())
            throw new UnsupportedOperationException("JSON Content is required.");

        StringEntity se = setMessageBody(json.toString());
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, MIME_TYPE_JSON));
        if (logger.isDebugEnabled())
        {
            logger.debug("Json string value: " + se);
        }
        return se;
    }
    
    /**
     * Parses http response stream into a {@link JSONObject}.
     * 
     * @param stream Http response entity
     * @return {@link JSONObject} response
     */
    public static JSONObject readStream(final HttpEntity entity)
    {
        String rsp = null;
        try
        {
            rsp = EntityUtils.toString(entity, "UTF-8");
        }
        catch (Throwable ex)
        {
            throw new RuntimeException("Failed to read HTTP entity stream.", ex);
        }
        finally
        {
            EntityUtils.consumeQuietly(entity);
        }
        try
        {
            JSONParser parser = new JSONParser();
            JSONObject result = (JSONObject) parser.parse(rsp);
            return result;
        }
        catch (Throwable e)
        {
            throw new RuntimeException(
                    "Failed to convert response to JSON: \n" +
                    "   Response: \r\n" + rsp,
                    e);
        }
    }
}
