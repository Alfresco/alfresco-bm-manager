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
package org.alfresco.bm.driver.file;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Concrete service implementation of {@link FileDataService} based on MongoDB.
 *
 * @author Derek Hulley
 * @since 1.4
 */
public class FileDataServiceImpl implements FileDataService, InitializingBean
{
    public static final String FIELD_FILESET = "fileset";
    public static final String FIELD_REMOTE_NAME = "remoteName";
    public static final String FIELD_LOCAL_NAME = "localName";
    public static final String FIELD_EXTENSION = "extension";
    public static final String FIELD_ENCODING = "encoding";
    public static final String FIELD_LOCALE = "locale";
    public static final String FIELD_SIZE = "size";
    public static final String FIELD_RANDOMIZER = "randomizer";
    
    private static final Log logger = LogFactory.getLog(FileDataServiceImpl.class);
    
    private DBCollection collection;
    
    public FileDataServiceImpl(DB db, String collection)
    {
        this.collection = db.getCollection(collection);
    }
    
    @Override
    public void afterPropertiesSet()
    {
        checkIndexes();
    }

    /**
     * Ensure that the MongoDB collection has the required indexes associated with
     * this user bean.
     */
    private void checkIndexes()
    {
        collection.setWriteConcern(WriteConcern.SAFE);
        
        DBObject idxRandomizer = BasicDBObjectBuilder
                .start(FIELD_FILESET, 1)
                .add(FIELD_RANDOMIZER, 1)
                .get();
        DBObject optRandomizer = BasicDBObjectBuilder
                .start("name", "idx_randomizer")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxRandomizer, optRandomizer);

        DBObject idxExtension = BasicDBObjectBuilder
                .start(FIELD_FILESET, 1)
                .add(FIELD_EXTENSION, 1)
                .add(FIELD_RANDOMIZER, 1)
                .get();
        DBObject optExtension = BasicDBObjectBuilder
                .start("name", "idx_extension")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxExtension, optExtension);

        DBObject uidxRemoteName = BasicDBObjectBuilder
                .start(FIELD_FILESET, 1)
                .add(FIELD_REMOTE_NAME, 1)
                .get();
        DBObject optRemoteName = BasicDBObjectBuilder
                .start("name", "uidx_remoteName")
                .add("unique", Boolean.TRUE)
                .get();
        collection.createIndex(uidxRemoteName, optRemoteName);

        DBObject uidxLocalName = BasicDBObjectBuilder
                .start(FIELD_FILESET, 1)
                .add(FIELD_LOCAL_NAME, 1)
                .get();
        DBObject optLocalName = BasicDBObjectBuilder
                .start("name", "uidx_localName")
                .add("unique", Boolean.TRUE)
                .get();
        collection.createIndex(uidxLocalName, optLocalName);
    }
    
    /**
     * Convert the Mongo DBObject into the API equivalent
     */
    private FileData fromDBObject(DBObject fileDataObj)
    {
        FileData ret = new FileData();
        ret.setFileset((String) fileDataObj.get(FIELD_FILESET));
        ret.setRemoteName((String) fileDataObj.get(FIELD_REMOTE_NAME));
        ret.setLocalName((String) fileDataObj.get(FIELD_LOCAL_NAME));
        ret.setExtension((String) fileDataObj.get(FIELD_EXTENSION));
        ret.setEncoding((String) fileDataObj.get(FIELD_ENCODING));
        ret.setLocale((String) fileDataObj.get(FIELD_LOCALE));
        ret.setSize((Long) fileDataObj.get(FIELD_SIZE));
        ret.setRandomizer((Integer) fileDataObj.get(FIELD_RANDOMIZER));
        return ret;
    }
    
    @Override
    public void createNewFileData(FileData fileData)
    {
        DBObject fileDataObj = BasicDBObjectBuilder.start()
                .add(FIELD_FILESET, fileData.getFileset())
                .add(FIELD_REMOTE_NAME, fileData.getRemoteName())
                .add(FIELD_LOCAL_NAME, fileData.getLocalName())
                .add(FIELD_EXTENSION, fileData.getExtension())
                .add(FIELD_ENCODING, fileData.getEncoding())
                .add(FIELD_LOCALE, fileData.getLocale())
                .add(FIELD_SIZE, fileData.getSize())
                .add(FIELD_RANDOMIZER, fileData.getRandomizer())
                .get();
        WriteResult result = collection.insert(fileDataObj);
        
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Wrote FileData to collection: \n" +
                    "   " + fileData + "\n" +
                    "   Result: " + result);
        }
    }

    @Override
    public long fileCount(String fileset)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_FILESET, fileset)
                .get();
        return collection.count(queryObj);
    }

    /**
     * Count the number of files of the given type in a fileset
     */
    private long fileCount(String fileset, String extension)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_FILESET, fileset)
                .add(FIELD_EXTENSION, extension)
                .get();
        return collection.count(queryObj);
    }

    @Override
    public FileData findFile(String fileset, String remoteName)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_FILESET, fileset)
                .add(FIELD_REMOTE_NAME, remoteName)
                .get();
        DBObject resultObj = collection.findOne(queryObj);
        if (resultObj == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Did not find file '" + remoteName + "' in " + fileset);
            }
            return null;
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Found file '" + remoteName + "' in " + fileset + ": " + resultObj);
            }
            return fromDBObject(resultObj);
        }
    }

    @Override
    public void removeFile(String fileset, String remoteName)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_FILESET, fileset)
                .add(FIELD_REMOTE_NAME, remoteName)
                .get();
        WriteResult result = collection.remove(queryObj);
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Removed " + fileset + "." + remoteName + " and hit " + result.getN() + " rows");
        }
    }

    @Override
    public FileData getRandomFile(String fileset)
    {
        long count = fileCount(fileset);
        if (count == 0L)
        {
            // There is nothing to choose from
            return null;
        }
        // Use a random number from 0 (inclusive) to 'count' (exclusive)
        int skip = (int) (Math.random() * (double) count);
        
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_FILESET, fileset)
                .get();
        DBCursor results = collection.find(queryObj).skip(skip).limit(1);
        if (results.size() == 0)
        {
            // No results
            return null;
        }
        else
        {
            DBObject fileDataObj = results.next();
            return fromDBObject(fileDataObj);
        }
    }

    @Override
    public FileData getRandomFile(String fileset, String extension)
    {
        long count = fileCount(fileset, extension);
        if (count == 0L)
        {
            // There is nothing to choose from
            return null;
        }
        // Use a random number from 0 (inclusive) to 'count' (exclusive)
        int skip = (int) (Math.random() * (double) count);
        
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_FILESET, fileset)
                .add(FIELD_EXTENSION, extension)
                .get();
        DBCursor results = collection.find(queryObj).skip(skip).limit(1);
        if (results.size() == 0)
        {
            // No results
            return null;
        }
        else
        {
            DBObject fileDataObj = results.next();
            return fromDBObject(fileDataObj);
        }
    }
}
