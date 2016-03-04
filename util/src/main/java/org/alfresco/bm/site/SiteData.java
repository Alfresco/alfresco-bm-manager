package org.alfresco.bm.site;

import java.io.Serializable;

import org.alfresco.bm.data.DataCreationState;

/**
 * Data representing a site.
 * 
 * @author steveglover
 * @author Michael Suzuki
 *
 */
public class SiteData implements Serializable
{
    private static final long serialVersionUID = -3774392026234649419L;

    public static final String FIELD_SITE_ID = "siteId";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESC = "description";
    public static final String FIELD_VISIBILITY = "visibility";
    public static final String FIELD_CREATION_STATE = "creationState";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_GUID = "guid";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_HAS_MEMBERS = "hasMembers";
    public static final String FIELD_RANDOMIZER = "randomizer";
    public static final String FIELD_DOMAIN = "domain";
    public static final String FIELD_PRESET = "preset";

    private DataCreationState creationState;
    private String domain;
    private String siteId;
    private String guid;
    private String path;
    private String sitePreset;
    private String title;
    private String description;
    private String visibility; // one of (PUBLIC,MODERATED,PRIVATE), defaults to PUBLIC
    private String type;
    private int randomizer;

    public SiteData()
    {
        randomizer = (int)(Math.random() * 1E6);
        creationState = DataCreationState.Unknown;
    }

    public DataCreationState getCreationState()
    {
        return creationState;
    }

    public void setCreationState(DataCreationState creationState)
    {
        this.creationState = creationState;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getGuid()
    {
        return guid;
    }

    public void setGuid(String guid)
    {
        this.guid = guid;
    }

    public int getRandomizer()
    {
        return randomizer;
    }

    public void setRandomizer(int randomizer)
    {
        this.randomizer = randomizer;
    }

    public String getDomain()
    {
        return domain;
    }

    public String getSiteId()
    {
        return siteId;
    }
    
    public String getSitePreset()
    {
        return sitePreset;
    }
    
    public String getTitle()
    {
        return title;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public String getVisibility() 
    {
        return visibility;
    }
    
    public String getType()
    {
        return type;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public void setSiteId(String siteId)
    {
        this.siteId = siteId;
    }

    public void setSitePreset(String sitePreset)
    {
        this.sitePreset = sitePreset;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setVisibility(String visibility)
    {
        this.visibility = visibility;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    @Override
    public String toString()
    {
        return "SiteData [creationState=" + creationState + ", domain=" + domain
                + ", siteId=" + siteId + ", guid=" + guid + ", path=" + path + ", sitePreset=" + sitePreset
                + ", title=" + title + ", description=" + description + ", visibility=" + visibility + ", type=" + type
                + ", randomizer=" + randomizer + "]";
    }
}
