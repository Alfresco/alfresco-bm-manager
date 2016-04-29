package org.alfresco.bm.user;

/**
 * Event data if to reschedule CheckUserCountEventProcessor
 * 
 * @author Frank Becker
 * @since 2.1.4
 */
public class CheckUserCountEventData
{
    private Object eventData;
    private long userCountCreated;
    private long userCountScheduled;

    /**
     * Constructor
     * 
     * @param originalEventData
     *        (Object, may be null) original event data as passed to event
     *        processor
     * @param usersCreated
     *        (long) number of users created
     * @param usersScheduled
     *        (long) number of users Scheduled
     */
    public CheckUserCountEventData(Object originalEventData, long usersCreated, long usersScheduled)
    {
        this.setEventData(originalEventData);
        this.setUserCountCreated(usersCreated);
        this.setUserCountScheduled(usersScheduled);
    }

    /**
     * @return the eventData
     */
    public Object getEventData()
    {
        return eventData;
    }

    /**
     * @param eventData (Object, may be null) the eventData to set
     */
    public void setEventData(Object eventData)
    {
        this.eventData = eventData;
    }

    /**
     * @return the number of users created
     */
    public long getUserCountCreated()
    {
        return userCountCreated;
    }

    /**
     * @param userCountCreated (long) the number of users created to set
     */
    public void setUserCountCreated(long userCountCreated)
    {
        this.userCountCreated = userCountCreated;
    }

    /**
     * @return the number of users scheduled for creation
     */
    public long getUserCountScheduled()
    {
        return userCountScheduled;
    }

    /**
     * @param userCountScheduled (long) number of users scheduled for creation to set
     */
    public void setUserCountScheduled(long userCountScheduled)
    {
        this.userCountScheduled = userCountScheduled;
    }
}
