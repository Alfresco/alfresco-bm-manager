package org.alfresco.bm.session;

import java.io.Serializable;

import com.google.gson.Gson;

/**
 * Default session data associated with a session. Subclass to persist further data.
 * 
 * @author steveglover
 *
 */
public class PersistedSessionData implements Serializable
{
	private static final long serialVersionUID = 2715736194608467334L;
	private static final Gson gson = new Gson();

	private long maxEndTime;

	public PersistedSessionData()
	{
		super();
	}

	public PersistedSessionData(long maxEndTime)
	{
		super();
		this.maxEndTime = maxEndTime;
	}

	public long getMaxEndTime()
	{
		return maxEndTime;
	}

    public static PersistedSessionData fromJSON(String json)
    {
        return gson.fromJson(json, PersistedSessionData.class);
    }
    
    /**
     * Helper to convert to JSON
     */
    public String toJSON()
    {
        return gson.toJson(this);
    }
	
}
