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
package org.alfresco.bm.file;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @see FileDataService
 * 
 * @author Derek Hulley
 * @since 1.3
 */
@RunWith(JUnit4.class)
public class FileDataServiceTest
{
    private final static String COLLECTION_BM_FILE_DATA_SERVICE = "BenchmarkFileDataServiceTest";
    private final static String FILESET_TEST = "TEST";
    
    private static AbstractApplicationContext ctx;
    private static FileDataService fileDataService;
    private static FtpTestFileService ftpTestFileService;
    private static LocalTestFileService localTestFileService;
    
    private static FileData[] fileDatas;

    @BeforeClass
    public static void setUp() throws IOException
    {
        // Create a test file
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File testFiles = new File(tempDir, "FileDataServiceTest");
        testFiles.mkdirs();
        File testFile = new File(testFiles, "test.txt");
        if (!testFile.exists())
        {
            String text = "SOME TEXT";
            Files.write(text, testFile, Charsets.UTF_8);
        }
        
        File localDir = new File(System.getProperty("java.io.tmpdir") + "/" + "fileset-123");
        if (localDir.exists())
        {
            localDir.delete();
        }
        
        Properties props = new Properties();
        props.put("test.mongoCollection", COLLECTION_BM_FILE_DATA_SERVICE);
        props.put("test.localDir", localDir.getCanonicalFile());
        props.put("test.ftpHost", "ftp.mirrorservice.org");
        props.put("test.ftpPort", "21");
        props.put("test.ftpUsername", "anonymous");
        props.put("test.ftpPassword", "");
        props.put("test.ftpPath", "/sites/www.linuxfromscratch.org/images");
        props.put("test.testFileDir", testFiles.getCanonicalPath());
        
        ctx = new ClassPathXmlApplicationContext(new String[] {"test-MongoFileDataTest-context.xml"}, false);
        ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("TestProps", props));
        ctx.refresh();
        ctx.start();

        // Get the new beans
        fileDataService = ctx.getBean(FileDataService.class);
        ftpTestFileService = ctx.getBean(FtpTestFileService.class);
        localTestFileService = ctx.getBean(LocalTestFileService.class);
        
        // Do a directory listing and use that as the dataset
        File randomDir = new File(System.getProperty("user.dir"));
        File[] localFiles = randomDir.listFiles(new FileFilter()
                {
                    @Override
                    public boolean accept(File pathname)
                    {
                        return !pathname.isDirectory();
                    }
                });
        fileDatas = new FileData[localFiles.length];
        for (int i = 0; i < localFiles.length; i++)
        {
            String remoteName = localFiles[i].getName();
            if (remoteName.length() == 0)
            {
                continue;
            }
            fileDatas[i] = FileDataServiceTest.createFileData(remoteName);
        }
    }
    
    @AfterClass
    public static void tearDown()
    {
        ctx.close();
    }
    
    /**
     * Create file data based on the remote store name
     */
    private static FileData createFileData(String remoteName)
    {
        String ext = FileData.getExtension(remoteName);
        String localName = UUID.randomUUID().toString() + "." + ext;
        
        FileData fileData = new FileData();
        fileData.setLocalName(localName);
        fileData.setRemoteName(remoteName);
        fileData.setExtension(ext);
        fileData.setFileset(FILESET_TEST);
        fileData.setSize(System.currentTimeMillis());
        
        return fileData;
    }
    
    /**
     * Ensures that the basic setup works
     */
    @Test
    public void testBasicCreate()
    {
        if (fileDataService.fileCount(FILESET_TEST) > 0L)
        {
            Assert.fail("We expect an empty dataset to start with.");
        }
        for (FileData fileData : fileDatas)
        {
            fileDataService.createNewFileData(fileData);
        }
        Assert.assertEquals("File count is incorrect. ", (long) fileDatas.length, fileDataService.fileCount(FILESET_TEST));
        Assert.assertEquals("Expect no files ", 0L, fileDataService.fileCount("blah"));
    }
    
    @Test
    public void testRandomFetch()
    {
        FileData fileData = fileDataService.getRandomFile(FILESET_TEST);
        Assert.assertNotNull("There are results but random file was not selected", fileData);
    }
    
    @Test
    public void testRandomFetchByExtension()
    {
        FileData fileData = fileDataService.getRandomFile(FILESET_TEST, "txt");
        Assert.assertNotNull("There are results but random file was not selected", fileData);
    }
    
    @Test
    public void testFetchByRemoteName()
    {
        String remoteName = fileDatas[0].getRemoteName();
        FileData fileData = fileDataService.findFile(FILESET_TEST, remoteName);
        Assert.assertNotNull("Did not find file by remote name", fileData);
        Assert.assertEquals(fileDatas[0].getRemoteName(), fileData.getRemoteName());
    }
    
    @Test
    public void testRemoveByRemoteName()
    {
        String remoteName = fileDatas[0].getRemoteName();
        FileData fileData = fileDataService.findFile(FILESET_TEST, remoteName);
        Assert.assertNotNull("Did not find file by remote name", fileData);
        Assert.assertEquals(fileDatas[0].getRemoteName(), remoteName);
        
        fileDataService.removeFile(FILESET_TEST, remoteName);
        fileData = fileDataService.findFile(FILESET_TEST, remoteName);
        Assert.assertNull("Did not remove file by remote name", fileData);
    }
    
    /**
     * Ensure that a test file can be retrieved
     */
    @Test
    public void testGetFile()
    {
        for (int i = 0; i < 100; i++)
        {
            File file = ftpTestFileService.getFile();
            Assert.assertNotNull("(FTP) Expected to find a test file.", file);
            Assert.assertTrue("(FTP) Test file does not exist.", file.exists());
            Assert.assertTrue("(FTP) Test file is empty.", file.length() > 0);
        }
    }
    
    /**
     * Ensure that a test file can be retrieved
     */
    @Test
    public void testGetFileWithExtension()
    {
        for (int i = 0; i < 100; i++)
        {
            File file = ftpTestFileService.getFile("png");
            Assert.assertNotNull("(FTP) Expected to find a test PNG file.", file);
            Assert.assertTrue("(FTP) Test PNG file does not exist.", file.exists());
            Assert.assertTrue("(FTP) Test PNG file is empty.", file.length() > 0);

            file = localTestFileService.getFile("txt");
            Assert.assertNotNull("(LOCAL) Expected to find a TXT file.", file);
            Assert.assertTrue("(LOCAL) Test TXT file does not exist.", file.exists());
            Assert.assertTrue("(LOCAL) Test TXT file is empty.", file.length() > 0);
        }
    }
    
    /**
     * Ensure that a test file can be retrieved
     */
    @Test
    public void testGetFileByName()
    {
        for (int i = 0; i < 100; i++)
        {
            File file = ftpTestFileService.getFileByName("lfs-logo.png");
            Assert.assertNotNull("(FTP) Expected to find a named file.", file);
            Assert.assertTrue("(FTP) Test PNG file does not exist.", file.exists());
            Assert.assertTrue("(FTP) Test PNG file is empty.", file.length() > 0);

            file = localTestFileService.getFileByName("test.txt");
            Assert.assertNotNull("(LOCAL) Expected to find a named file.", file);
            Assert.assertTrue("(LOCAL) Test TXT file does not exist.", file.exists());
            Assert.assertTrue("(LOCAL) Test TXT file is empty.", file.length() > 0);
        }
    }
}
