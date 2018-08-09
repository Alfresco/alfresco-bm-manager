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
package org.alfresco.bm.publicapi.factory;

import org.springframework.social.alfresco.api.Alfresco;

/**
 * A factory for creating a public api connection for a specific user.
 * 
 * @author steveglover
 *
 */
public interface PublicApiFactory
{
    /**
     * Get a public api connection for the "username"
     * 
     * @param username the user name of the user
     * @return a public api connection for the given user
     * @throws Exception
     */
    Alfresco getPublicApi(String username);
    
    /**
     * Get a public api connection for the admin user of the given domain.
     * 
     * Note: may not be implemented by all factories.
     * 
     * @param domain        the domain id of the domain admin user
     * @return              a public api connection for the admin user of the given domain
     * @throws Exception
     */
    Alfresco getTenantAdminPublicApi(String domain);
    
    /**
     * Get a public api connection for the admin user.
     * 
     * Note: may not be implemented by all factories.
     * 
     * @return a public api connection for the admin user
     * @throws Exception
     */
    Alfresco getAdminPublicApi();
}
