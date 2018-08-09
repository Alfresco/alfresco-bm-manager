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
package org.alfresco.bm.site;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.publicapi.factory.SiteException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteConcern;

/**
 * Service providing access to {@link SiteData} and {@link SiteMemberData}
 * storage. All {@link SiteData} and {@link SiteMemberData} returned from and
 * persisted with this service will be test-run-specific. The
 * test-run-identifier is set in the constructor.
 * 
 * Note: code moved and modified from public API benchmark
 * 
 * @author Steve Glover
 * @author Michael Suzuki
 * @author Frank Becker
 * 
 * @since 2.1.2
 */
public class SiteDataServiceImpl implements SiteDataService, InitializingBean
{
    private static Log logger = LogFactory.getLog(SiteDataServiceImpl.class);
    private DBCollection sitesCollection;
    private DBCollection siteMembersCollection;

    public SiteDataServiceImpl(DB db, String sites, String siteMembers)
    {
        this.sitesCollection = db.getCollection(sites);
        this.siteMembersCollection = db.getCollection(siteMembers);
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        checkIndexes();
    }

    /**
     * Ensure that the MongoDB collection has the required indexes associated
     * with
     * this user bean.
     */
    public void checkIndexes()
    {
        sitesCollection.setWriteConcern(WriteConcern.SAFE);

        DBObject idxSiteId = BasicDBObjectBuilder.start()
                .append(SiteData.FIELD_SITE_ID, 1)
                .get();
        DBObject optSiteId = BasicDBObjectBuilder.start()
                .append("name", "idx_SiteId")
                .append("unique", Boolean.TRUE)
                .get();
        sitesCollection.createIndex(idxSiteId, optSiteId);

        DBObject idxSiteCreated = BasicDBObjectBuilder.start()
                .append(SiteData.FIELD_CREATION_STATE, 1)
                .append(SiteMemberData.FIELD_RANDOMIZER, 1)
                .get();
        DBObject optIdxSiteCreated = BasicDBObjectBuilder.start()
                .append("name", "idx_SiteCreated")
                .append("unique", Boolean.FALSE)
                .get();
        sitesCollection.createIndex(idxSiteCreated, optIdxSiteCreated);

        siteMembersCollection.setWriteConcern(WriteConcern.SAFE);

        DBObject idxSiteMember = BasicDBObjectBuilder.start()
                .append(SiteMemberData.FIELD_SITE_ID, 1)
                .append(SiteMemberData.FIELD_USERNAME, 1)
                .append("unique", Boolean.TRUE)
                .get();
        DBObject optSiteMember = BasicDBObjectBuilder.start()
                .append("name", "idx_SiteMember")
                .append("unique", Boolean.TRUE)
                .get();
        siteMembersCollection.createIndex(idxSiteMember, optSiteMember);

        DBObject idxSiteCreatedRoleRand = BasicDBObjectBuilder.start()
                .append(SiteMemberData.FIELD_SITE_ID, 1)
                .append(SiteMemberData.FIELD_CREATION_STATE, 1)
                .append(SiteMemberData.FIELD_ROLE, 1)
                .append(SiteMemberData.FIELD_RANDOMIZER, 1)
                .get();
        DBObject optSiteCreatedRoleRand = BasicDBObjectBuilder.start()
                .append("name", "idxSiteCreatedRoleRand")
                .append("unique", Boolean.FALSE)
                .get();
        siteMembersCollection.createIndex(idxSiteCreatedRoleRand,
                optSiteCreatedRoleRand);
    }

    /**
     * Converts {@link DBObject} to {@link SiteData}.
     */
    public static SiteData convertSiteDataDBObject(DBObject result)
    {
        if (result == null)
        {
            return null;
        }
        Integer rand = (Integer) result.get(SiteData.FIELD_RANDOMIZER);
        if (rand == null)
        {
            logger.warn("Found site data without randomizer field: " + result);
            rand = -1;
        }
        String DataCreationStateStr = (String) result
                .get(SiteData.FIELD_CREATION_STATE);
        DataCreationState creationState = DataCreationState.Unknown;
        try
        {
            creationState = DataCreationState.valueOf(DataCreationStateStr);
        }
        catch (Exception e)
        {
            logger.error("Site data has unknown state: " + result);
        }

        SiteData site = new SiteData();
        site.setRandomizer(rand);
        site.setDomain((String) result.get(SiteData.FIELD_DOMAIN));
        site.setSiteId((String) result.get(SiteData.FIELD_SITE_ID));
        site.setGuid((String) result.get(SiteData.FIELD_GUID));
        site.setPath((String) result.get(SiteData.FIELD_PATH));
        site.setSitePreset((String) result.get(SiteData.FIELD_PRESET));
        site.setTitle((String) result.get(SiteData.FIELD_TITLE));
        site.setDescription((String) result.get(SiteData.FIELD_DESC));
        site.setVisibility((String) result.get(SiteData.FIELD_VISIBILITY));
        site.setType((String) result.get(SiteData.FIELD_TYPE));
        site.setCreationState(creationState);
        return site;
    }

    /**
     * Converts {@link SiteData} to {@link DBObject}.
     */
    public static DBObject convertSiteData(SiteData siteData)
    {
        if (siteData == null)
        {
            throw new IllegalArgumentException("'siteData' may not be null.");
        }
        return BasicDBObjectBuilder.start()
                .append(SiteData.FIELD_SITE_ID, siteData.getSiteId())
                .append(SiteData.FIELD_GUID, siteData.getGuid())
                .append(SiteData.FIELD_DOMAIN, siteData.getDomain())
                .append(SiteData.FIELD_PATH, siteData.getPath())
                .append(SiteData.FIELD_PRESET, siteData.getSitePreset())
                .append(SiteData.FIELD_TITLE, siteData.getTitle())
                .append(SiteData.FIELD_DESC, siteData.getDescription())
                .append(SiteData.FIELD_VISIBILITY, siteData.getVisibility())
                .append(SiteData.FIELD_RANDOMIZER, siteData.getRandomizer())
                .append(SiteData.FIELD_CREATION_STATE,
                        siteData.getCreationState().toString())
                .get();
    }

    /**
     * Converts {@link DBObject} to {@link SiteMemberData}.
     */
    public static SiteMemberData convertSiteMemberDBObject(DBObject result)
    {
        if (result == null)
        {
            return null;
        }
        Integer rand = (Integer) result.get(SiteMemberData.FIELD_RANDOMIZER);
        if (rand == null)
        {
            logger.warn("Found site data without randomizer field: " + result);
            rand = -1;
        }
        String DataCreationStateStr = (String) result
                .get(SiteData.FIELD_CREATION_STATE);
        DataCreationState creationState = DataCreationState.Unknown;
        try
        {
            creationState = DataCreationState.valueOf(DataCreationStateStr);
        }
        catch (Exception e)
        {
            logger.error("Site data has unknown state: " + result);
        }
        SiteMemberData siteMember = new SiteMemberData();
        siteMember.setRandomizer(rand);
        siteMember.setCreationState(creationState);
        siteMember.setUsername(
                (String) result.get(SiteMemberData.FIELD_USERNAME));
        siteMember.setSiteId((String) result.get(SiteMemberData.FIELD_SITE_ID));
        siteMember.setRole((String) result.get(SiteMemberData.FIELD_ROLE));
        // Done
        return siteMember;
    }

    /**
     * Converts {@link SiteMemberData} to {@link DBObject}.
     */
    public static DBObject convertSiteMember(SiteMemberData siteMember)
    {
        if (siteMember == null)
        {
            throw new IllegalArgumentException("'siteMember' may not be null.");
        }
        return BasicDBObjectBuilder.start()
                .append(SiteMemberData.FIELD_USERNAME, siteMember.getUsername())
                .append(SiteMemberData.FIELD_SITE_ID, siteMember.getSiteId())
                .append(SiteMemberData.FIELD_ROLE, siteMember.getRole())
                .append(SiteMemberData.FIELD_CREATION_STATE,
                        siteMember.getCreationState().toString())
                .append(SiteMemberData.FIELD_RANDOMIZER,
                        siteMember.getRandomizer())
                .get();
    }

    @Override
    public long countSites(String domain, DataCreationState state)
    {
        DBObject queryObj = new BasicDBObject();
        if (state != null)
        {
            queryObj.put(SiteData.FIELD_CREATION_STATE, state.toString());
        }
        if (domain != null)
        {
            queryObj.put(SiteData.FIELD_DOMAIN, domain);
        }
        return sitesCollection.count(queryObj);
    }

    /**
     * Find site in Site collection.
     * 
     * @param siteId
     *        identifier
     * @return result of query
     */
    private DBObject findSite(String siteId)
    {
        if (siteId == null || siteId.isEmpty())
        {
            throw new RuntimeException("SiteId is required");
        }
        return sitesCollection
                .findOne(new BasicDBObject(SiteData.FIELD_SITE_ID, siteId));
    }

    @Override
    public SiteData getSite(String siteId)
    {
        DBObject result = findSite(siteId);
        return convertSiteDataDBObject(result);
    }

    @Override
    public void addSite(SiteData newSite)
    {
        DBObject data = convertSiteData(newSite);
        sitesCollection.insert(data);
    }

    @Override
    public void setSiteCreationState(String siteId, String guid,
            DataCreationState state)
    {
        DBObject findObj = new BasicDBObject(SiteData.FIELD_SITE_ID, siteId);
        DBObject updateObj = BasicDBObjectBuilder
                .start()
                .push("$set")
                .add(SiteData.FIELD_CREATION_STATE, state.toString())
                .add(SiteData.FIELD_GUID, guid)
                .pop()
                .get();
        DBObject newObj = sitesCollection.findAndModify(findObj, null, null,
                false, updateObj, true, false);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Updated site: " + newObj);
        }
    }

    @Override
    public List<SiteData> getSites(String domain, DataCreationState state,
            int skip, int count)
    {
        List<SiteData> sites = new ArrayList<SiteData>(count);

        DBObject queryObj = new BasicDBObject();
        if (state != null)
        {
            queryObj.put(SiteData.FIELD_CREATION_STATE, state.toString());
        }
        if (domain != null)
        {
            queryObj.put(SiteData.FIELD_DOMAIN, domain);
        }

        DBCursor cursor = sitesCollection.find(queryObj).skip(skip)
                .limit(count);
        try
        {
            while (cursor.hasNext())
            {
                SiteData site = convertSiteDataDBObject(cursor.next());
                sites.add(site);
            }
        }
        finally
        {
            cursor.close();
        }
        return sites;
    }

    private Integer getMaxSiteRandomizer()
    {
        return getSiteDataRandomizer(false);
    }

    private Integer getMinSiteRandomizer()
    {
        return getSiteDataRandomizer(true);
    }

    /**
     * Gets max or minimum value of randomizer value of SiteData in collection.
     * To get highest query pass 1 or -1 for lowest.
     * 
     * @param direction
     *        ascend or descend
     * @return {@link Integer} randomizer value
     */
    private Integer getSiteDataRandomizer(boolean ascending)
    {
        int direction = ascending ? 1 : -1;
        DBObject fieldsObj = new BasicDBObject(SiteData.FIELD_RANDOMIZER, true);
        DBObject sortObj = new BasicDBObject(SiteData.FIELD_RANDOMIZER,
                direction);
        DBObject resultObj = sitesCollection.findOne(null, fieldsObj, sortObj);
        return resultObj == null ? null
                : (Integer) resultObj.get(SiteData.FIELD_RANDOMIZER);
    }

    @Override
    public SiteData randomSite(String domain, DataCreationState state)
    {
        SiteData site = null;

        BasicDBObject query = new BasicDBObject();
        if (domain != null)
        {
            query.append(SiteData.FIELD_DOMAIN, domain);
        }
        if (state != null)
        {
            query.append(SiteData.FIELD_CREATION_STATE, state.toString());
        }

        Integer max = getMaxSiteRandomizer();
        Integer min = getMinSiteRandomizer();
        if (min != null && max != null)
        {
            int r = (int) (Math.random() * (max - min)) + min;
            query.append(SiteData.FIELD_RANDOMIZER,
                    new BasicDBObject("$gt", r));
            DBObject result = sitesCollection.findOne(query);
            site = convertSiteDataDBObject(result);
            if (site == null)
            {
                query.put(SiteData.FIELD_RANDOMIZER,
                        new BasicDBObject("$lt", r));
                result = sitesCollection.findOne(query);
                site = convertSiteDataDBObject(result);
            }
        }
        return site;
    }

    @Override
    public long countSiteMembers(String siteId, DataCreationState state)
    {
        BasicDBObject queryObj = new BasicDBObject();
        if (siteId != null)
        {
            queryObj.append(SiteMemberData.FIELD_SITE_ID, siteId);
        }
        if (state != null)
        {
            queryObj.append(SiteMemberData.FIELD_CREATION_STATE,
                    state.toString());
        }
        return siteMembersCollection.count(queryObj);
    }

    @Override
    public void addSiteMember(SiteMemberData siteMember)
    {
        DBObject data = convertSiteMember(siteMember);
        try
        {
            siteMembersCollection.insert(data);
        }
        catch (Exception e)
        {
            throw new RuntimeException(
                    "Failed to insert site member: " + siteMember, e);
        }
    }

    @Override
    public List<SiteMemberData> getSiteMembers(String siteId,
            DataCreationState state, String role, int skip, int count)
    {
        List<SiteMemberData> members = new ArrayList<SiteMemberData>();
        BasicDBObject queryObj = new BasicDBObject();
        if (siteId != null)
        {
            queryObj.append(SiteMemberData.FIELD_SITE_ID, siteId);
        }
        if (state != null)
        {
            queryObj.append(SiteMemberData.FIELD_CREATION_STATE,
                    state.toString());
        }
        if (role != null)
        {
            queryObj.append(SiteMemberData.FIELD_ROLE, role);
        }

        DBCursor cursor = siteMembersCollection.find(queryObj).skip(skip)
                .limit(count);
        try
        {
            while (cursor.hasNext())
            {
                SiteMemberData member = convertSiteMemberDBObject(
                        cursor.next());
                members.add(member);
            }
        }
        catch (Exception e)
        {
            logger.warn("Unable to get site members", e);
        }
        finally
        {
            cursor.close();
        }
        return members;
    }

    @Override
    public SiteMemberData randomSiteMember(String siteId,
            DataCreationState state, String username, String... roles)
    {
        QueryBuilder queryObjBuilder = QueryBuilder
                .start(SiteMemberData.FIELD_SITE_ID).exists(Boolean.TRUE);
        if (siteId != null)
        {
            queryObjBuilder.and(SiteMemberData.FIELD_SITE_ID).is(siteId);
        }
        if (state != null)
        {
            queryObjBuilder.and(SiteMemberData.FIELD_CREATION_STATE)
                    .is(state.toString());
        }
        if (username != null)
        {
            queryObjBuilder.and(SiteMemberData.FIELD_USERNAME).is(username);
        }
        if (roles != null && roles.length > 0)
        {
            queryObjBuilder.and(SiteMemberData.FIELD_ROLE).in(roles);
        }
        BasicDBObject query = (BasicDBObject) queryObjBuilder.get();    // Need
                                                                        // to
                                                                        // cast
                                                                        // for
                                                                        // later
                                                                        // puts
        int random = (int) (Math.random() * (double) 1e6);
        query.put(SiteData.FIELD_RANDOMIZER,
                new BasicDBObject("$gte", Integer.valueOf(random)));

        DBObject result = siteMembersCollection.findOne(query);
        if (result == null)
        {
            result = siteMembersCollection.findOne();
            query.put(SiteData.FIELD_RANDOMIZER,
                    new BasicDBObject("$lt", random));
            result = siteMembersCollection.findOne(query);
        }
        return convertSiteMemberDBObject(result);
    }

    @Override
    public boolean isSiteMember(String siteId, String userId)
    {
        SiteMemberData siteMember = getSiteMember(siteId, userId);
        return siteMember != null;
    }

    @Override
    public SiteMemberData getSiteMember(String siteId, String userId)
    {
        DBObject query = new BasicDBObject()
                .append(SiteMemberData.FIELD_SITE_ID, siteId)
                .append(SiteMemberData.FIELD_USERNAME, userId);
        DBObject result = siteMembersCollection.findOne(query);
        return convertSiteMemberDBObject(result);
    }

    @Override
    public void setSiteMemberCreationState(String siteId, String userId,
            DataCreationState state)
    {
        DBObject findObj = new BasicDBObject()
                .append(SiteMemberData.FIELD_USERNAME, userId)
                .append(SiteMemberData.FIELD_SITE_ID, siteId);
        DBObject updateObj = BasicDBObjectBuilder
                .start()
                .push("$set")
                .add(SiteMemberData.FIELD_CREATION_STATE, state.toString())
                .pop()
                .get();
        siteMembersCollection.findAndModify(findObj, null, null, false,
                updateObj, false, false);
    }

    @Override
    public long countSites(String domain, DataCreationState state,
            long minSitesRequired) throws SiteException
    {
        long siteCount = countSites(domain, state);
        if (siteCount < minSitesRequired)
        {
            throw new SiteException(
                    "Minimum sites required: " + minSitesRequired
                            + ", but only " + siteCount + " sites created!");
        }
        return siteCount;
    }
}
