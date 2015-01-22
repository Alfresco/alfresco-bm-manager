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
    public static final String FIELD_ROOT = "root";
    public static final String FIELD_PATH = "path";
//    public static final String FIELD_PARENT_PATH = "parentPath";      This field is not required at present
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
        
        DBObject uidxRootPath = BasicDBObjectBuilder.start()
                .add(FIELD_ROOT, 1)
                .add(FIELD_PATH, 1)
                .get();
        DBObject optRootPath = BasicDBObjectBuilder.start()
                .add("name", "uidxRootPath")
                .add("unique", Boolean.TRUE)
                .get();
        collection.createIndex(uidxRootPath, optRootPath);

//        DBObject uidxRootParentName = BasicDBObjectBuilder.start()
//                .add(FIELD_ROOT, 1)
//                .add(FIELD_PARENT_PATH, 1)
//                .add(FIELD_NAME, 1)
//                .get();
//        DBObject optRootParentName = BasicDBObjectBuilder.start()
//                .add("name", "uidxRootParentName")
//                .add("unique", Boolean.FALSE)
//                .get();
//        collection.createIndex(uidxRootParentName, optRootParentName);
//
        DBObject idxRootFileCount = BasicDBObjectBuilder.start()
                .add(FIELD_ROOT, 1)
                .add(FIELD_FILE_COUNT, 1)
                .get();
        DBObject optRootFileCount = BasicDBObjectBuilder.start()
                .add("name", "idxRootFileCount")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxRootFileCount, optRootFileCount);

        DBObject idxRootFolderCount = BasicDBObjectBuilder.start()
                .add(FIELD_ROOT, 1)
                .add(FIELD_FOLDER_COUNT, 1)
                .get();
        DBObject optRootFolderCount = BasicDBObjectBuilder.start()
                .add("name", "idxRootFolderCount")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxRootFolderCount, optRootFolderCount);
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
        String root = (String) folderDataObj.get(FIELD_ROOT);
        String path = (String) folderDataObj.get(FIELD_PATH);
        Long folderCount = (Long) folderDataObj.get(FIELD_FOLDER_COUNT);
        Long fileCount = (Long) folderDataObj.get(FIELD_FILE_COUNT);
        FolderData folderData = new FolderData(id, root, path, folderCount, fileCount);
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
    public void createNewFolder(String id, String root, String path)
    {
        FolderData folder = new FolderData(id, root, path, 0L, 0L);
        createNewFolder(folder);
    }

    /**
     * Create a new folder entry with the given data
     */
    public void createNewFolder(FolderData data)
    {
        BasicDBObjectBuilder insertObjBuilder = BasicDBObjectBuilder.start()
                .add(FIELD_ID, data.getId())
                .add(FIELD_ROOT, data.getRoot())
                .add(FIELD_PATH, data.getPath())
//                .add(FIELD_PARENT_PATH, data.getParentPath())
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
     * @param root              the root that the folder is in
     * @param path              the path from the root
     * @return                  the folder data or <tt>null</tt> if it does not exist
     */
    public FolderData getFolder(String root, String path)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_ROOT, root)
                .add(FIELD_PATH, path)
                .get();
        DBObject folderDataObj = collection.findOne(queryObj);
        FolderData folderData = fromDBObject(folderDataObj);
        return folderData;
    }
    
    /**
     * Increment the count of the subfolders in a folder.
     * 
     * @param root              the root that the folder is in
     * @param path              the path from the root
     * @param fileCountInc      the file count increment (can be negative)
     */
    public void incrementFolderCount(String root, String path, long folderCountInc)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_ROOT, root)
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
                    "   Root:   " + root + "\n" +
                    "   Path:   " + path + "\n" +
                    "   Result:   " + result);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Incremented the subfolder count on " + root + "/" + path + " by " + folderCountInc);
        }
    }
    
    /**
     * Increment the count of the files in a folder.
     * 
     * @param root              the root that the folder is in
     * @param path              the path from the root
     * @param fileCountInc      the file count increment (can be negative)
     */
    public void incrementFileCount(String root, String path, long fileCountInc)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_ROOT, root)
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
                    "   Root:   " + root + "\n" +
                    "   Path:   " + path + "\n" +
                    "   Result:   " + result);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Incremented the file count on " + root + "/" + path + " by " + fileCountInc);
        }
    }
    
//    /**
//     * Produces a count of the folders that have the given root and path as a <b>parent</b> folder.
//     * <p/>
//     * If the following files exist:
//     * <pre>
//     *      /home/tests/a
//     *      /home/tests/b
//     * </pre>
//     * then the counts for:
//     * <pre>
//     *      /home/tests
//     * </pre>
//     * will be <tt>2</tt>
//     * 
//     * @return      the folder count
//     */
//    public long countChildFolders(String root, String path)
//    {
//        DBObject queryObj = BasicDBObjectBuilder.start()
//                .add(FIELD_ROOT, root)
//                .add(FIELD_PARENT_PATH, path)
//                .get();
//        long count = collection.count(queryObj);
//        // Done
//        if (logger.isDebugEnabled())
//        {
//            logger.debug("Count of children for " + root + path + ": " + count);
//        }
//        return count;
//    }
//    
//    /**
//     * Get a list of folders that have the given root and path as a <b>parent</b> folder.
//     * <p/>
//     * If the path given is:
//     * <pre>
//     *      /home/tests
//     * </pre>
//     * then the following results can be returned:
//     * <pre>
//     *      /home/tests/a
//     *      /home/tests/b
//     * </pre>
//     * 
//     * @param root              the root that the folder is in
//     * @param path              the path that will be the parent of all child folders returned
//     * @param skip              the number of entries to skip
//     * @param limit             the number of entries to return
//     * @return                  the child folders
//     */
//    public List<FolderData> getChildFolders(String root, String path, int skip, int limit)
//    {
//        DBObject queryObj = BasicDBObjectBuilder.start()
//                .add(FIELD_ROOT, root)
//                .add(FIELD_PARENT_PATH, path)
//                .get();
//        DBObject sortObj = BasicDBObjectBuilder.start()
//                .add(FIELD_ROOT, 1)
//                .add(FIELD_PARENT_PATH, 1)
//                .get();
//        DBCursor cursor = collection.find(queryObj).sort(sortObj).skip(skip).limit(limit);
//        List<FolderData> results = fromDBCursor(cursor);
//        // Done
//        if (logger.isDebugEnabled())
//        {
//            logger.debug("Found " + results.size() + " results in folder " + root + path);
//        }
//        return results;
//    }
//    
    /**
     * Get a list of folders filtered by the number of child files
     * 
     * @param root              the root that the folder is in (mandatory)
     * @param path              the path that will be the parent of all child folders returned (optional).
     *                          The given folder at the given path and <b>all subfolders</b> will be included.
     * @param minFolders        the minimum number of subfolders in the folder (inclusive, optional)
     * @param maxFolders        the maximum number of subfolders in the folder (inclusive, optional)
     * @param skip              the number of entries to skip
     * @param limit             the number of entries to return
     * @return                  the folders with the correct number of children
     */
    public List<FolderData> getFoldersByFolderCounts(
            String root, String path,
            Long minFolders, Long maxFolders,
            int skip, int limit)
    {
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start()
                .add(FIELD_ROOT, root);
        if (path != null)
        {
            queryObjBuilder.add(FIELD_PATH, java.util.regex.Pattern.compile("^" + path));
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
            }
            queryObjBuilder.pop();
        }
        DBObject queryObj = queryObjBuilder.get();
        
        DBObject sortObj = null;
        if (path != null)
        {
            sortObj = BasicDBObjectBuilder.start()
                    .add(FIELD_ROOT, 1)
                    .add(FIELD_PATH, 1)
                    .get();
        }
        else
        {
            sortObj = BasicDBObjectBuilder.start()
                    .add(FIELD_ROOT, 1)
                    .add(FIELD_FOLDER_COUNT, 1)
                    .get();
        }
        
        DBCursor cursor = collection.find(queryObj).sort(sortObj).skip(skip).limit(limit);
        List<FolderData> results = fromDBCursor(cursor);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Found " + results.size() + " results for file counts: \n" +
                    "   root:       " + root + "\n" +
                    "   path:       " + path + "\n" +
                    "   minFolders: " + minFolders + "\n" +
                    "   maxFolders: " + maxFolders + "\n" +
                    "   skip:       " + skip + "\n" +
                    "   limit:      " + limit);
        }
        return results;
    }

    /**
     * Get a list of folders filtered by the number of child files
     * 
     * @param root              the root that the folder is in (mandatory)
     * @param path              the path that will be the parent of all child folders returned (optional).
     *                          The given folder at the given path and <b>all subfolders</b> will be included.
     * @param minFiles          the minimum number of files in the folder (inclusive, optional)
     * @param maxFiles          the maximum number of files in the folder (inclusive, optional)
     * @param skip              the number of entries to skip
     * @param limit             the number of entries to return
     * @return                  the folders with the correct number of children
     */
    public List<FolderData> getFoldersByFileCounts(
            String root, String path,
            Long minFiles, Long maxFiles,
            int skip, int limit)
    {
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start()
                .add(FIELD_ROOT, root);
        if (path != null)
        {
            queryObjBuilder.add(FIELD_PATH, java.util.regex.Pattern.compile("^" + path));
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
            }
            queryObjBuilder.pop();
        }
        DBObject queryObj = queryObjBuilder.get();
        
        DBObject sortObj = null;
        if (path != null)
        {
            sortObj = BasicDBObjectBuilder.start()
                    .add(FIELD_ROOT, 1)
                    .add(FIELD_PATH, 1)
                    .get();
        }
        else
        {
            sortObj = BasicDBObjectBuilder.start()
                    .add(FIELD_ROOT, 1)
                    .add(FIELD_FILE_COUNT, 1)
                    .get();
        }
        
        DBCursor cursor = collection.find(queryObj).sort(sortObj).skip(skip).limit(limit);
        List<FolderData> results = fromDBCursor(cursor);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Found " + results.size() + " results for file counts: \n" +
                    "   root:       " + root + "\n" +
                    "   path:       " + path + "\n" +
                    "   minFiles:   " + minFiles + "\n" +
                    "   maxFiles:   " + maxFiles + "\n" +
                    "   skip:       " + skip + "\n" +
                    "   limit:      " + limit);
        }
        return results;
    }
}
