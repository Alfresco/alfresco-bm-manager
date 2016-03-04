package org.alfresco.bm.site;

import java.io.Serializable;

import org.alfresco.bm.data.DataCreationState;

/**
 * Data representing a site member.
 * 
 * @author steveglover
 * @author Michael Suzuki
 * @author Derek Hulley
 */
public class SiteMemberData implements Serializable
{
    private static final long serialVersionUID = 505331886661880389L;

    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_SITE_ID = "siteId";
    public static final String FIELD_CREATION_STATE = "creationState";
    public static final String FIELD_RANDOMIZER = "randomizer";
    public static final String FIELD_ROLE = "role";

    private DataCreationState creationState;
    private String username;
    private String siteId;
    private String role;
    private int randomizer;

    public SiteMemberData()
    {
        this.randomizer = (int)(Math.random() * 1E6);
        this.creationState = DataCreationState.Unknown;
    }
    
    public void setRandomizer(int randomizer)
    {
        this.randomizer = randomizer;
    }

    public DataCreationState getCreationState()
    {
        return creationState;
    }

    public void setCreationState(DataCreationState creationState)
    {
        this.creationState = creationState;
    }

    public int getRandomizer()
    {
        return randomizer;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getSiteId()
    {
        return siteId;
    }

    public void setSiteId(String siteId)
    {
        this.siteId = siteId;
    }

    @Override
    public String toString()
    {
        return "SiteMember [creationState=" + creationState + ", username=" + username + ", siteId=" + siteId
                + ", role=" + role + ", randomizer=" + randomizer + "]";
    }
}
