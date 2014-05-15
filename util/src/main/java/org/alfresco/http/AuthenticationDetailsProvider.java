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
package org.alfresco.http;

/**
 * Provides details to allow users to authenticate against alfresco.
 *
 * @author Frederik Heremans
 * @since 1.2
 */
public interface AuthenticationDetailsProvider
{
    /**
     * @return the password for the given user. Returns null, if user doesn't exist.
     */
    String getPasswordForUser(String username);
    
    /**
     * @return  the ticket being used by the given user or <tt>null</tt> if no ticket
     *          is stored
     */
    String getTicketForUser(String username);
    
    /**
     * Update the value of the ticket for the given user.
     */
    void updateTicketForUser(String username, String ticket);
    
    /**
     * @return the Alfresco administrator username
     */
    String getAdminUsername();
    
    
    /**
     * @return the Alfresco administrator password
     */
    String getAdminPassword();
}
