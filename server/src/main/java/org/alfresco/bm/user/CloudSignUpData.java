/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
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
 * POJO representing a user Cloud signup data.
 * 
 * @author Derek Hulley
 * @since 1.2
 */
public class CloudSignUpData implements Serializable
{
    private static final long serialVersionUID = 7899398626404496696L;
    
    private String id;
    private String key;
    private boolean complete;

    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }
    public String getKey()
    {
        return key;
    }
    public void setKey(String key)
    {
        this.key = key;
    }
    public boolean isComplete()
    {
        return complete;
    }
    public void setComplete(boolean complete)
    {
        this.complete = complete;
    }
}
