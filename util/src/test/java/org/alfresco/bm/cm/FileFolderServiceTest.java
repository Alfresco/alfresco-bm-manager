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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.mongo.MongoDBForTestsFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException.DuplicateKey;

/**
 * @see FileFolderService
 * 
 * @author Derek Hulley
 * @since 4.0.3
 */
@SuppressWarnings("deprecation")
@RunWith(JUnit4.class)
public class FileFolderServiceTest
{
    private static MongoDBForTestsFactory mongoFactory;
    private FileFolderService fileFolderService;
    private DB db;
    private DBCollection ffs;
    
    @Before
    public void setUp() throws Exception
    {
        mongoFactory = new MongoDBForTestsFactory();
        db = mongoFactory.getObject();
        fileFolderService = new FileFolderService(db, "ffs");
        fileFolderService.afterPropertiesSet();
        ffs = db.getCollection("ffs");
    }
    
    @After
    public void tearDown() throws Exception
    {
        mongoFactory.destroy();
    }
    
    @Test
    public void basic()
    {
        assertNotNull(db);
        assertNotNull(ffs);
        Set<String> collectionNames = new HashSet<String>();
        collectionNames.add("system.indexes");
        collectionNames.add("ffs");
        assertEquals(collectionNames, db.getCollectionNames());
        
        // Check indexes (includes implicit '_id_' index)
        List<DBObject> indexes = ffs.getIndexInfo();
        assertEquals("Incorrect indexes: " + indexes, 4, indexes.size());
    }
    
    @Test
    public void parentPath()
    {
        FolderData folderData = new FolderData("R1", "home", "/myfolders", 0L, 0L);
        assertEquals("/", folderData.getParentPath());
        assertEquals("myfolders", folderData.getName());
    }
    
    @Test
    public void createAndRead()
    {
        FolderData folderData = new FolderData("A123", "home", "/myfolders/tests", 6L, 17L);
        assertEquals("tests", folderData.getName());
        assertEquals("/myfolders", folderData.getParentPath());
        // Persist
        fileFolderService.createNewFolder(folderData);
        // Retrieve by ID
        FolderData folderDataCheck = fileFolderService.getFolder("A123");
        assertEquals(folderData, folderDataCheck);
        assertEquals(17L, folderDataCheck.getFileCount());
        // Retrieve by path
        folderDataCheck = fileFolderService.getFolder("home", "/myfolders/tests");
        assertEquals(folderData, folderDataCheck);
        
        // Make sure we don't get any errors if there is nothing
        assertNull(fileFolderService.getFolder("B123"));
        assertNull(fileFolderService.getFolder("home", "/myFolders/tests"));
        assertNull(fileFolderService.getFolder("home", "/myfolders/tests2"));
        assertNull(fileFolderService.getFolder("away", "/myfolders/tests"));
    }
    
    @Test(expected=DuplicateKey.class)
    public void uniqueId()
    {
        FolderData folderData = new FolderData("A123", "home", "/myfolders/tests", 6L, 17L);
        fileFolderService.createNewFolder(folderData);

        FolderData folderData2 = new FolderData("A123", "home", "/myfolders/reports", 6L, 5L);
        fileFolderService.createNewFolder(folderData2);
    }
    
    @Test(expected=DuplicateKey.class)
    public void uniquePath()
    {
        FolderData folderData = new FolderData("A123", "home", "/myfolders/tests", 3L, 17L);
        fileFolderService.createNewFolder(folderData);

        FolderData folderData2 = new FolderData("B456", "home", "/myfolders/tests", 2L, 5L);
        fileFolderService.createNewFolder(folderData2);
    }
    
    @Test
    public void increment()
    {
        FolderData folderData = new FolderData("A123", "home", "/myfolders/tests", 3L, 17L);
        fileFolderService.createNewFolder(folderData);
        fileFolderService.incrementFolderCount("home", "/myfolders/tests", 3L);
        fileFolderService.incrementFileCount("home", "/myfolders/tests", 3L);
        FolderData folderDataCheck = fileFolderService.getFolder("A123");
        assertEquals(6L, folderDataCheck.getFolderCount());
        assertEquals(20L, folderDataCheck.getFileCount());
    }
//    
//    @Test
//    public void childFolderCounts()
//    {
//        fileFolderService.createNewFolder("a", "home", "/a");
//        fileFolderService.createNewFolder("aa", "home", "/a/a");
//        fileFolderService.createNewFolder("ab", "home", "/a/b");
//        fileFolderService.createNewFolder("ac", "home", "/a/c");
//        fileFolderService.createNewFolder("aca", "home", "/a/c/a");
//        fileFolderService.createNewFolder("acb", "home", "/a/c/b");
//        fileFolderService.createNewFolder("", "home", "/b");
//        
//        assertEquals(3L, fileFolderService.countChildFolders("home", "/a"));
//        assertEquals(0L, fileFolderService.countChildFolders("home", "/a/a"));
//        assertEquals(2L, fileFolderService.countChildFolders("home", "/a/c"));
//        assertEquals(2L, fileFolderService.countChildFolders("home", "/"));
//    }
//    
//    @Test
//    public void folderLists()
//    {
//        fileFolderService.createNewFolder("a", "home", "/a");
//        fileFolderService.createNewFolder("aa", "home", "/a/a");
//        fileFolderService.createNewFolder("ab", "home", "/a/b");
//        fileFolderService.createNewFolder("ac", "home", "/a/c");
//        fileFolderService.createNewFolder("aca", "home", "/a/c/a");
//        fileFolderService.createNewFolder("acb", "home", "/a/c/b");
//        fileFolderService.createNewFolder("", "home", "/b");
//        
//        assertEquals(3L, fileFolderService.getChildFolders("home", "/a", 0, 10).size());
//        assertEquals(1L, fileFolderService.getChildFolders("home", "/a", 0, 1).size());
//        assertEquals(2L, fileFolderService.getChildFolders("home", "/a", 1, 10).size());
//        assertEquals(0L, fileFolderService.getChildFolders("home", "/a/a", 0, 10).size());
//        assertEquals(2L, fileFolderService.getChildFolders("home", "/a/c", 0, 10).size());
//        assertEquals(2L, fileFolderService.getChildFolders("home", "/", 0, 10).size());
//    }
    
    @Test
    public void getFoldersByFolderCounts()
    {
        fileFolderService.createNewFolder("a", "home", "/a");
        fileFolderService.createNewFolder("aa", "home", "/a/a");
        fileFolderService.createNewFolder("Aa", "home", "/A/a");
        fileFolderService.createNewFolder("ab", "home", "/a/b");
        fileFolderService.createNewFolder("ac", "home", "/a/c");
        fileFolderService.createNewFolder("aca", "home", "/a/c/a");
        fileFolderService.createNewFolder("acb", "home", "/a/c/b");
        fileFolderService.createNewFolder("", "home", "/b");
        
        fileFolderService.incrementFolderCount("home", "/a/a", 20L);
        fileFolderService.incrementFolderCount("home", "/A/a", 10L);
        fileFolderService.incrementFolderCount("home", "/a/b", 10L);
        fileFolderService.incrementFolderCount("home", "/a/c", 30L);
        
        assertEquals(1, fileFolderService.getFoldersByFolderCounts("home", "/A", 0L, 100L, 0, 10).size());
        assertEquals(6, fileFolderService.getFoldersByFolderCounts("home", "/a", 0L, 100L, 0, 10).size());
        assertEquals(2, fileFolderService.getFoldersByFolderCounts("home", "/a", 10L, 20L, 0, 10).size());
        // Path sorting if used
        assertEquals("/a/a", fileFolderService.getFoldersByFolderCounts("home", "/a", 10L, 20L, 0, 1).get(0).getPath());
        assertEquals("/a/b", fileFolderService.getFoldersByFolderCounts("home", "/a", 10L, 20L, 1, 1).get(0).getPath());
        // Check case-sensitive path sorting
        assertEquals("/A/a", fileFolderService.getFoldersByFolderCounts("home", "", 10L, 20L, 0, 1).get(0).getPath());
        assertEquals("/a/a", fileFolderService.getFoldersByFolderCounts("home", "", 10L, 20L, 1, 1).get(0).getPath());
        assertEquals("/a/b", fileFolderService.getFoldersByFolderCounts("home", "", 10L, 20L, 2, 1).get(0).getPath());
        // Numerical sorting when path not present
        assertEquals("/a/a", fileFolderService.getFoldersByFolderCounts("home", null, 10L, 30L, 2, 1).get(0).getPath());
        assertEquals("/a/c", fileFolderService.getFoldersByFolderCounts("home", null, 10L, 30L, 3, 1).get(0).getPath());
    }
    
    @Test
    public void getFoldersByFileCounts()
    {
        fileFolderService.createNewFolder("a", "home", "/a");
        fileFolderService.createNewFolder("aa", "home", "/a/a");
        fileFolderService.createNewFolder("Aa", "home", "/A/a");
        fileFolderService.createNewFolder("ab", "home", "/a/b");
        fileFolderService.createNewFolder("ac", "home", "/a/c");
        fileFolderService.createNewFolder("aca", "home", "/a/c/a");
        fileFolderService.createNewFolder("acb", "home", "/a/c/b");
        fileFolderService.createNewFolder("", "home", "/b");
        
        fileFolderService.incrementFileCount("home", "/a/a", 20L);
        fileFolderService.incrementFileCount("home", "/A/a", 10L);
        fileFolderService.incrementFileCount("home", "/a/b", 10L);
        fileFolderService.incrementFileCount("home", "/a/c", 30L);
        
        assertEquals(1, fileFolderService.getFoldersByFileCounts("home", "/A", 0L, 100L, 0, 10).size());
        assertEquals(6, fileFolderService.getFoldersByFileCounts("home", "/a", 0L, 100L, 0, 10).size());
        assertEquals(2, fileFolderService.getFoldersByFileCounts("home", "/a", 10L, 20L, 0, 10).size());
        // Path sorting if used
        assertEquals("/a/a", fileFolderService.getFoldersByFileCounts("home", "/a", 10L, 20L, 0, 1).get(0).getPath());
        assertEquals("/a/b", fileFolderService.getFoldersByFileCounts("home", "/a", 10L, 20L, 1, 1).get(0).getPath());
        // Check case-sensitive path sorting
        assertEquals("/A/a", fileFolderService.getFoldersByFileCounts("home", "", 10L, 20L, 0, 1).get(0).getPath());
        assertEquals("/a/a", fileFolderService.getFoldersByFileCounts("home", "", 10L, 20L, 1, 1).get(0).getPath());
        assertEquals("/a/b", fileFolderService.getFoldersByFileCounts("home", "", 10L, 20L, 2, 1).get(0).getPath());
        // Numerical sorting when path not present
        assertEquals("/a/a", fileFolderService.getFoldersByFileCounts("home", null, 10L, 30L, 2, 1).get(0).getPath());
        assertEquals("/a/c", fileFolderService.getFoldersByFileCounts("home", null, 10L, 30L, 3, 1).get(0).getPath());
    }
}
