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

import java.util.List;

import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.publicapi.factory.SiteException;

/**
 * Service interface providing mirror data for Alfresco sites
 * 
 * @author Steven Glover
 * @author Derek Hulley
 * @author Frank Becker
 * 
 * @since 2.1.2
 */
public interface SiteDataService
{
    /**
     * Utility method to find a site by siteId
     */
    SiteData getSite(String siteId);

    /**
     * Add a site
     */
    void addSite(SiteData newSite);

    /**
     * Mark the site as created or not created
     */
    void setSiteCreationState(String siteId, String guid,
            DataCreationState state);

    /**
     * Counts the number of sites in the given domain.
     * 
     * @param domain
     *        Domain or null to get sites from all domains
     * @param state
     *        creation state or null to get all states
     * 
     * @return the number of sites in the given domain
     */
    long countSites(String domain, DataCreationState state);

    /**
     * Counts the number of sites in the given domain.
     * 
     * @param domain
     *        Domain or null to get sites from all domains
     * @param state
     *        creation state or null to get all states
     * @param minSitesRequired
     *        minimum number of sites required - will throw SiteException if
     *        created sites are less
     *        
     * @return the number of sites in the given domain
     */
    long countSites(String domain, DataCreationState state,
            long minSitesRequired) throws SiteException;

    /**
     * Get a paged list of sites
     * 
     * @param domain
     *        filter on the domain or <tt>null</tt> to ignore
     * @param state
     *        filter on the creation state or <tt>null</tt> to ignore
     * 
     * @return a paged collection of sites
     */
    List<SiteData> getSites(String domain, DataCreationState state, int skip,
            int count);

    /**
     * Get a random site for the given parameters
     * 
     * @param domain
     *        the user domain (optional)
     * @param state
     *        the membership creation state (optional)
     * @return a random site
     */
    SiteData randomSite(String domain, DataCreationState state);

    /**
     * Count the site members for the given criteria
     * 
     * @param siteId
     *        the id of the site (optional)
     * @param state
     *        the membership creation state (optional)
     * @return the site membership could
     */
    long countSiteMembers(String siteId, DataCreationState state);

    /**
     * Add a site member, initially not created.
     */
    void addSiteMember(SiteMemberData siteMember);

    /**
     * Get a random site member in the site given by "siteId" and state
     * 
     * @param siteId
     *        the ID of the site (optional)
     * @param state
     *        the data creation state of the site (optional)
     * @param username
     *        a specific user that must be a member (optional)
     * @param roles
     *        specific site roles to find (optional)
     * @return a random member of the given site
     */
    SiteMemberData randomSiteMember(String siteId, DataCreationState state,
            String username, String... roles);

    /**
     * Get a specific site member
     */
    public SiteMemberData getSiteMember(String siteId, String userId);

    /**
     * Get a paged list of site members for the given parameters
     * 
     * @param siteId
     *        the name of the site (optional)
     * @param state
     *        the creation state (optional)
     * @param role
     *        the member's or members' role (optional)
     * 
     * @return a list of site members matching the criteria
     */
    List<SiteMemberData> getSiteMembers(String siteId, DataCreationState state,
            String role, int skip, int count);

    /**
     * Is userId a member of the site "siteId"?
     * 
     * @param siteId
     *        site id
     * @param userId
     *        site member id
     * @return true if the user is a member of the site, false otherwise
     */
    boolean isSiteMember(String siteId, String userId);

    /**
     * Mark the site member as created or not created
     */
    void setSiteMemberCreationState(String siteId, String userId,
            DataCreationState state);
}
