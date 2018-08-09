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
package org.alfresco.bm.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alfresco.bm.data.DataCreationState;

/**
 * Data representing a single user.
 * 
 * @author Frederik Heremans
 * @author Derek Hulley
 * @since 1.1
 */
public class UserData implements Serializable
{
    private static final long serialVersionUID = 6819295613500413737L;
    
    private int randomizer;
    private String username;
    private String password;
    private DataCreationState creationState;
    private String firstName;
    private String lastName;
    private String email;
    private String domain;
    private List<String> groups;
    
    public UserData()
    {
        randomizer = (int)(Math.random() * 1E6);
        creationState = DataCreationState.Unknown;
        groups = Collections.emptyList();
    }

    public int getRandomizer()
    {
        return randomizer;
    }
    /**
     * Persistence only
     */
    @SuppressWarnings("unused")
    private void setRandomizer(int randomizer)
    {
        this.randomizer = randomizer;
    }

    public String getUsername()
    {
        return username;
    }
    public void setUsername(String username)
    {
        this.username = username;
    }
    
    public String getPassword()
    {
        return password;
    }
    public void setPassword(String password)
    {
        this.password = password;
    }

    public DataCreationState getCreationState()
    {
        return creationState;
    }
    public void setCreationState(DataCreationState creationState)
    {
        this.creationState = creationState;
    }

    public String getFirstName()
    {
        return firstName;
    }
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }
    
    public String getLastName()
    {
        return lastName;
    }
    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }
    
    public String getEmail()
    {
        return email;
    }
    public void setEmail(String email)
    {
        this.email = email;
    }
    
    public String getDomain()
    {
        return domain;
    }
    public void setDomain(String domain)
    {
        this.domain = domain;
    }
    
    public List<String> getGroups()
    {
        return groups;
    }
    public void setGroups(List<String> groups)
    {
        this.groups = Collections.unmodifiableList(new ArrayList<String>(groups));
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("UserData [username=");
        builder.append(username);
        builder.append(", creationState=");
        builder.append(creationState);
        builder.append(", email=");
        builder.append(email);
        builder.append(", domain=");
        builder.append(domain);
        builder.append(", groups=");
        builder.append(groups);
        builder.append("]");
        return builder.toString();
    }
}
