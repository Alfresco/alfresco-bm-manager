package org.alfresco.bm.api.v1;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.bm.util.ArgumentCheck;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.sun.jersey.json.impl.JSONHelper;

/**
 * Event details to show in the BM UI
 * 
 * @author Frank Becker
 * @since 2.0.10
 */
public class EventDetails implements Serializable
{
    /** Serialization ID */
    private static final long serialVersionUID = -5522236271295974192L;

    /** Stores the default name of event details */
    public static final String DEFAULT_EVENT_DETAILS_NAME_STRING = "(default)";

    /**
     * Default constructor
     */
    public EventDetails()
    {
        this.setEventTime(new Date())
                .setEventName(DEFAULT_EVENT_DETAILS_NAME_STRING)
                .setEventSuccess(false)
                .setEventInputData(null)
                .setEventResultData(null);
    }

    /**
     * STD constructor
     * 
     * @param time
     *            (Date) event time
     * @param name
     *            (String) event name
     * @param success
     *            (boolean) event succeeded?
     * @param inputData
     *            (Object, optional) event.data object that created the event result
     * @param resultData
     *            (Object, optional) eventResult.data object
     */
    public EventDetails(Date time, String name, boolean success, Object inputData, Object resultData)
    {
        this.setEventTime(time)
                .setEventName(name)
                .setEventSuccess(success)
                .setEventInputData(inputData)
                .setEventResultData(resultData);
    }
    /** Stores the event time */
    private Date eventTime;

    /**
     * @return the eventTime
     */
    public Date getEventTime()
    {
        return eventTime;
    }

    /**
     * @param eventTime
     *            (Date, required) the eventTime to set
     */
    public EventDetails setEventTime(Date eventTime)
    {
        ArgumentCheck.checkMandatoryObject(eventTime, "eventTime");
        this.eventTime = eventTime;
        return this;
    }

    /** Stores the event name */
    private String eventName;

    /**
     * @return (String) the eventName
     */
    public String getEventName()
    {
        return eventName;
    }

    /**
     * @param eventName
     *            (String, required) the eventName to set
     */
    public EventDetails setEventName(String eventName)
    {
        ArgumentCheck.checkMandatoryString(eventName, "eventName");
        this.eventName = eventName;
        return this;
    }

    /** Stores the event success */
    private boolean eventSuccess;

    /**
     * @return (boolean) the eventSuccess
     */
    public boolean isEventSuccess()
    {
        return eventSuccess;
    }

    /**
     * @param eventSuccess
     *            (boolean) the eventSuccess to set
     */
    public EventDetails setEventSuccess(boolean eventSuccess)
    {
        this.eventSuccess = eventSuccess;
        return this;
    }

    /** Stores the input data of the event (event.data field as string representation) */
    private String eventInputData;

    /**
     * @return (String, may be null or empty) input data of the event (event.data field as string representation)
     */
    public String getEventInputData()
    {
        return eventInputData;
    }

    /**
     * @param eventInputData
     *            (Object, optional) input data of the event (event.data field as string representation) to set
     */
    public EventDetails setEventInputData(Object eventInputData)
    {
        this.eventInputData = (null != eventInputData) ? eventInputData.toString() : "";
        return this;
    }

    /** Stores the string representation (or null / empty String) of the EventResult.data field. */
    private String eventResultData;

    /**
     * @return (String, may be null or empty) the string representation (or null / empty String) of the EventResult.data
     *         field
     */
    public String getEventResultData()
    {
        return eventResultData;
    }

    /**
     * @param eventResultData
     *            (Object, optional) the string representation (or null / empty String) of the EventResult.data field to
     *            set
     */
    public EventDetails setEventResultData(Object eventResultData)
    {
        this.eventResultData = (null != eventResultData) ? eventResultData.toString() : "";
        return this;
    }

    /**
     * @return (String) JSON serialization of this class
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("EventDetails ");
        builder.append("[ eventTime=").append(eventTime);
        builder.append(", eventName=").append(eventName);
        builder.append(", eventSuccess=").append(eventSuccess);
        builder.append(", eventInputData=").append(eventInputData);
        builder.append(", eventResultData=").append(eventResultData);
        builder.append("]");
        return builder.toString();
    }
    
    /**
     * Conversion to DBObject
     *  
     * @return DBObject
     */
    public DBObject toDBObject()
    {
        DBObject eventObj = BasicDBObjectBuilder
                .start()
                .add("eventTime", eventTime)
                .add("eventName", eventName)
                .add("eventSuccess", eventSuccess)
                .add("eventInputData", eventInputData)
                .add("eventResultData", eventResultData)
                .get();
                
        return eventObj;
    }
}
