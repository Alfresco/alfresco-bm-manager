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
