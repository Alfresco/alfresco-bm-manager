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
package org.alfresco.bm.cm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * Service to keep track of files and folders.
 *
 * @author Derek Hulley
 * @since 4.0.4
 */
public class FileFolderService implements InitializingBean
{
    public static final String FIELD_ID = "_id";
    public static final String FIELD_CONTEXT = "context";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_LEVEL = "level";
    public static final String FIELD_PARENT_PATH = "parentPath";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_FOLDER_COUNT = "folderCount";
    public static final String FIELD_FILE_COUNT = "fileCount";
    
    private static Log logger = LogFactory.getLog(FileFolderService.class);

    /** The collection of users, which can be reused by derived extensions. */
    protected final DBCollection collection;
    
    public FileFolderService(DB db, String collection)
    {
        this.collection = db.getCollection(collection);
    }
    
    @Override
    public void afterPropertiesSet() throws Exception
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
        
        DBObject uidxCtxPath = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, 1)
                .add(FIELD_PATH, 1)
                .get();
        DBObject optCtxPath = BasicDBObjectBuilder.start()
                .add("name", "uidxCtxPath")
                .add("unique", Boolean.TRUE)
                .get();
        collection.createIndex(uidxCtxPath, optCtxPath);

        DBObject uidxCtxParentName = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, 1)
                .add(FIELD_PARENT_PATH, 1)
                .add(FIELD_NAME, 1)
                .get();
        DBObject optCtxParentName = BasicDBObjectBuilder.start()
                .add("name", "uidxCtxParentName")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(uidxCtxParentName, optCtxParentName);

        DBObject idxCtxLevel = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, 1)
                .add(FIELD_LEVEL, 1)
                .get();
        DBObject optCtxLevel = BasicDBObjectBuilder.start()
                .add("name", "idxCtxLevel")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxCtxLevel, optCtxLevel);
        
        DBObject idxCtxFileCount = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, 1)
                .add(FIELD_FILE_COUNT, 1)
                .add(FIELD_LEVEL, 1)
                .get();
        DBObject optCtxFileCount = BasicDBObjectBuilder.start()
                .add("name", "idxCtxFileCount")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxCtxFileCount, optCtxFileCount);

        DBObject idxCtxFolderCount = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, 1)
                .add(FIELD_FOLDER_COUNT, 1)
                .add(FIELD_LEVEL, 1)
                .get();
        DBObject optCtxFolderCount = BasicDBObjectBuilder.start()
                .add("name", "idxCtxFolderCount")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxCtxFolderCount, optCtxFolderCount);
    }
    
    /**
     * Helper to convert a Mongo DBObject into the API consumable object
     * <p/>
     * Note that <tt>null</tt> is handled as a <tt>null</tt> return.
     */
    protected FolderData fromDBObject(DBObject folderDataObj)
    {
        if (folderDataObj == null)
        {
            return null;
        }
        
        String id = (String) folderDataObj.get(FIELD_ID);
        String context = (String) folderDataObj.get(FIELD_CONTEXT);
        String path = (String) folderDataObj.get(FIELD_PATH);
        Long folderCount = (Long) folderDataObj.get(FIELD_FOLDER_COUNT);
        Long fileCount = (Long) folderDataObj.get(FIELD_FILE_COUNT);
        FolderData folderData = new FolderData(id, context, path, folderCount, fileCount);
        // Done
        return folderData;
    }
    
    /**
     * Turn a cursor into an array of API-friendly objects
     */
    protected List<FolderData> fromDBCursor(DBCursor cursor)
    {
        int count = cursor.count();
        try
        {
            List<FolderData> folderDatas = new ArrayList<FolderData>(count);
            while (cursor.hasNext())
            {
                DBObject folderDataObj = cursor.next();
                FolderData folderData = fromDBObject(folderDataObj);
                folderDatas.add(folderData);
            }
            // Done
            return folderDatas;
        }
        finally
        {
            cursor.close();
        }
    }
    
    /**
     * Create a new folder entry with the given data.
     * <p/>
     * The file count is assumed to be zero.
     */
    public void createNewFolder(String id, String context, String path)
    {
        FolderData folder = new FolderData(id, context, path, 0L, 0L);
        createNewFolder(folder);
    }

    /**
     * Create a new folder entry with the given data
     */
    public void createNewFolder(FolderData data)
    {
        BasicDBObjectBuilder insertObjBuilder = BasicDBObjectBuilder.start()
                .add(FIELD_ID, data.getId())
                .add(FIELD_CONTEXT, data.getContext())
                .add(FIELD_PATH, data.getPath())
                .add(FIELD_LEVEL, data.getLevel())
                .add(FIELD_PARENT_PATH, data.getParentPath())
                .add(FIELD_NAME, data.getName())
                .add(FIELD_FOLDER_COUNT, data.getFolderCount())
                .add(FIELD_FILE_COUNT, data.getFileCount());
        DBObject insertObj = insertObjBuilder.get();
        
        try
        {
            collection.insert(insertObj);
        }
        catch (DuplicateKeyException e)
        {
            // We just rethrow as per the API
            throw e;
        }
    }
    
    /**
     * Delete a folder entry based on the folder ID
     * 
     * @param id                the folder ID
     */
    public int deleteFolder(String context, String path, boolean cascade)
    {
        int deleted = 0;
        // Delete primary
        {
            DBObject queryObj = BasicDBObjectBuilder.start()
                    .add(FIELD_CONTEXT, context)
                    .add(FIELD_PATH, path)
                    .get();
            WriteResult wr = collection.remove(queryObj);
            deleted += wr.getN();
        }
        // Cascade
        if (cascade)
        {
            DBObject queryObj = BasicDBObjectBuilder.start()
                    .add(FIELD_CONTEXT, context)
                    .push(FIELD_PATH)
                        .add("$regex", "^" + path + "/")
                    .pop()
                    .get();
            WriteResult wr = collection.remove(queryObj);
            deleted += wr.getN();
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Deleted folder: \n" +
                    "   Context:        " + context + "\n" +
                    "   Path:           " + path + "\n" +
                    "   Cascade:        " + cascade + "\n" +
                    "   Deleted:        " + deleted);
        }
        return deleted;
    }
    
    /**
     * Retrieve a folder by the ID
     * 
     * @param id                the folder id
     * @return                  the folder data or <tt>null</tt> if it does not exist
     */
    public FolderData getFolder(String id)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_ID, id)
                .get();
        DBObject folderDataObj = collection.findOne(queryObj);
        FolderData folderData = fromDBObject(folderDataObj);
        return folderData;
    }
    
    /**
     * Retrieve a folder by the path
     * 
     * @param context           the context in which the folder path is valid (mandatory)
     * @param path              the folder path relative to the given context
     * @return                  the folder data or <tt>null</tt> if it does not exist
     */
    public FolderData getFolder(String context, String path)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, context)
                .add(FIELD_PATH, path)
                .get();
        DBObject folderDataObj = collection.findOne(queryObj);
        FolderData folderData = fromDBObject(folderDataObj);
        return folderData;
    }
    
    /**
     * Increment the count of the subfolders in a folder.
     * 
     * @param context           the context in which the folder path is valid (mandatory)
     * @param path              the folder path relative to the given context
     * @param fileCountInc      the file count increment (can be negative)
     */
    public void incrementFolderCount(String context, String path, long folderCountInc)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, context)
                .add(FIELD_PATH, path)
                .get();
        DBObject updateObj = BasicDBObjectBuilder.start()
                .push("$inc")
                    .add(FIELD_FOLDER_COUNT, folderCountInc)
                .pop()
                .get();
        WriteResult result = collection.update(queryObj, updateObj);
        if (result.getN() != 1)
        {
            throw new RuntimeException(
                    "Failed to update folder's subfolder count: \n" +
                    "   Context:  " + context + "\n" +
                    "   Path:     " + path + "\n" +
                    "   Result:   " + result);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Incremented the subfolder count on " + context + "/" + path + " by " + folderCountInc);
        }
    }
    
    /**
     * Increment the count of the files in a folder.
     * 
     * @param context           the context in which the folder path is valid (mandatory)
     * @param path              the folder path relative to the given context
     * @param fileCountInc      the file count increment (can be negative)
     */
    public void incrementFileCount(String context, String path, long fileCountInc)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, context)
                .add(FIELD_PATH, path)
                .get();
        DBObject updateObj = BasicDBObjectBuilder.start()
                .push("$inc")
                    .add(FIELD_FILE_COUNT, fileCountInc)
                .pop()
                .get();
        WriteResult result = collection.update(queryObj, updateObj);
        if (result.getN() != 1)
        {
            throw new RuntimeException(
                    "Failed to update folder's file count: \n" +
                    "   Context:  " + context + "\n" +
                    "   Path:     " + path + "\n" +
                    "   Result:   " + result);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Incremented the file count on " + context + "/" + path + " by " + fileCountInc);
        }
    }
    
    /**
     * Produces a count of the folders that have the given context and path as a <b>parent</b> folder.
     * <p/>
     * If the following files exist:
     * <pre>
     *      /home/tests/a
     *      /home/tests/b
     * </pre>
     * then the counts for:
     * <pre>
     *      /home/tests
     * </pre>
     * will be <tt>2</tt>
     * 
     * @return      the folder count
     */
    public long countChildFolders(String context, String path)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, context)
                .add(FIELD_PARENT_PATH, path)
                .get();
        long count = collection.count(queryObj);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Count of children for " + context + path + ": " + count);
        }
        return count;
    }
    
    /**
     * Count folders without either subfolders or files
     * 
     * @return                  count of folders without subfolders or files
     */
    public long countEmptyFolders(String context)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, context)
                .add(FIELD_FOLDER_COUNT, 0L)
                .add(FIELD_FILE_COUNT, 0L)
                .get();
        long count = collection.count(queryObj);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("There are " + count + " empty folders in context '" + context + "'.");
        }
        return count;
    }
    
    /**
     * Get a list of folders that have the given context and path as a <b>parent</b> folder.
     * <p/>
     * If the path given is:
     * <pre>
     *      /home/tests
     * </pre>
     * then the following results can be returned:
     * <pre>
     *      /home/tests/a
     *      /home/tests/b
     * </pre>
     * 
     * @param context           the context in which the folder path is valid (mandatory)
     * @param path              the path that will be the parent of all child folders returned
     * @param skip              the number of entries to skip
     * @param limit             the number of entries to return
     * @return                  the child folders
     */
    public List<FolderData> getChildFolders(String context, String path, int skip, int limit)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, context)
                .add(FIELD_PARENT_PATH, path)
                .get();
        DBObject sortObj = BasicDBObjectBuilder.start()
                .add(FIELD_CONTEXT, 1)
                .add(FIELD_PARENT_PATH, 1)
                .get();
        DBCursor cursor = collection.find(queryObj).sort(sortObj).skip(skip).limit(limit);
        List<FolderData> results = fromDBCursor(cursor);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Found " + results.size() + " results in folder " + context + path);
        }
        return results;
    }

    /**
     * Get a list of folders filtered by the number of child files and/or folders, returning
     * results sorted according to the parameters supplied.
     * <p/>
     * Apart from the context, all parametes are optional.  However, for best performance,
     * do not mix the file and folder levels; the underlying query performance will be OK
     * but the sorting will not be ideal.
     * <p/>
     * The sort precedence is <b>folderCount-fileCount</b>.
     * 
     * @param context           the context in which the folder path is valid (mandatory)
     * @param minLevel          the minimum folder level to consider (inclusive, optional)
     * @param maxLevel          the maximum folder level to consider (inclusive, optional)
     * @param minFiles          the minimum number of files in the folder (inclusive, optional)
     * @param maxFiles          the maximum number of files in the folder (inclusive, optional)
     * @param skip              the number of entries to skip
     * @param limit             the number of entries to return
     * @return                  the folders with the correct number of children
     */
    public List<FolderData> getFoldersByCounts(
            String context,
            Long minLevel, Long maxLevel,
            Long minFolders, Long maxFolders,
            Long minFiles, Long maxFiles,
            int skip, int limit)
    {
        if (context == null)
        {
            throw new IllegalArgumentException();
        }
        
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start();
        BasicDBObjectBuilder sortObjBuilder = BasicDBObjectBuilder.start();
        
        queryObjBuilder.add(FIELD_CONTEXT, context);
        if (minLevel != null || maxLevel != null)
        {
            queryObjBuilder.push(FIELD_LEVEL);
            {
                if (minLevel != null)
                {
                    queryObjBuilder.add("$gte", minLevel);
                }
                if (maxLevel != null)
                {
                    queryObjBuilder.add("$lte", maxLevel);
                }
                // No sorting by level!
            }
            queryObjBuilder.pop();
        }
        if (minFolders != null || maxFolders != null)
        {
            queryObjBuilder.push(FIELD_FOLDER_COUNT);
            {
                if (minFolders != null)
                {
                    queryObjBuilder.add("$gte", minFolders);
                }
                if (maxFolders != null)
                {
                    queryObjBuilder.add("$lte", maxFolders);
                }
                // We have to sort by the counts
                sortObjBuilder.add(FIELD_FOLDER_COUNT, 1);
            }
            queryObjBuilder.pop();
        }
        if (minFiles != null || maxFiles != null)
        {
            queryObjBuilder.push(FIELD_FILE_COUNT);
            {
                if (minFiles != null)
                {
                    queryObjBuilder.add("$gte", minFiles);
                }
                if (maxFiles != null)
                {
                    queryObjBuilder.add("$lte", maxFiles);
                }
                // We have to sort by the counts
                sortObjBuilder.add(FIELD_FILE_COUNT, 1);
            }
            queryObjBuilder.pop();
        }
        DBObject queryObj = queryObjBuilder.get();
        DBObject sortObj = sortObjBuilder.get();
        
        DBCursor cursor = collection.find(queryObj).sort(sortObj).skip(skip).limit(limit);
        List<FolderData> results = fromDBCursor(cursor);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Found " + results.size() + " results for file counts: \n" +
                    "   context:    " + context + "\n" +
                    "   minLevel:   " + minLevel + "\n" +
                    "   maxLevel:   " + maxLevel + "\n" +
                    "   minFiles:   " + minFiles + "\n" +
                    "   maxFiles:   " + maxFiles + "\n" +
                    "   skip:       " + skip + "\n" +
                    "   limit:      " + limit);
        }
        return results;
    }
}
