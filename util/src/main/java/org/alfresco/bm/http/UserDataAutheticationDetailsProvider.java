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
package org.alfresco.bm.http;

import org.alfresco.bm.user.UserData;
import org.alfresco.bm.user.UserDataService;
import org.alfresco.http.AuthenticationDetailsProvider;
import org.springframework.beans.factory.InitializingBean;

/**
 * {@link AuthenticationDetailsProvider} using {@link UserData} to store user-details.
 *
 * @author Frederik Heremans
 * @since 1.2
 */
public class UserDataAutheticationDetailsProvider implements AuthenticationDetailsProvider, InitializingBean
{
    private final UserDataService userDataService;
    private final String adminUserName;
    private final String adminPassword;

    /**
     * @param userDataService service to use for {@link UserData} related operations
     */
    public UserDataAutheticationDetailsProvider(UserDataService userDataService, String adminUserName, String adminPassword)
    {
        this.userDataService = userDataService;
        this.adminUserName = adminUserName;
        this.adminPassword = adminPassword;
    }

    /**
     * Ensures that the bean is properly initialized.
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
    }

    @Override
    public String getPasswordForUser(String username)
    {
        UserData user = userDataService.findUserByUsername(username);
        if(user != null)
        {
            return user.getPassword();
        }
        return null;
    }

    @Override
    public String getTicketForUser(String userName)
    {
        UserData user = userDataService.findUserByUsername(userName);
        if(user != null)
        {
            return user.getTicket();
        }
        return null;
    }

    @Override
    public void updateTicketForUser(String username, String ticket) throws IllegalArgumentException
    {
        userDataService.setUserTicket(username, ticket);
    }

    @Override
    public String getAdminUsername()
    {
        return this.adminUserName;
    }

    @Override
    public String getAdminPassword()
    {
        return this.adminPassword;
    }
}
