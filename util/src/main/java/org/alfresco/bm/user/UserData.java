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
package org.alfresco.bm.user;

import java.io.Serializable;

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
    private boolean created;
    private String firstName;
    private String lastName;
    private String email;
    private String domain;
    private String ticket;
    private String nodeId;
    
    public UserData()
    {
        randomizer = (int)(Math.random() * 1E6);
        created = false;
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

    public boolean isCreated()
    {
        return this.created;
    }
    public void setCreated(boolean created)
    {
        this.created = created;
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
    
    public String getTicket()
    {
        return ticket;
    }
    public void setTicket(String ticket)
    {
        this.ticket = ticket;
    }

    public String getNodeId()
    {
        return nodeId;
    }
    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("UserData [username=");
        builder.append(username);
        builder.append(", created=");
        builder.append(created);
        builder.append(", email=");
        builder.append(email);
        builder.append(", domain=");
        builder.append(domain);
        builder.append("]");
        return builder.toString();
    }
}
