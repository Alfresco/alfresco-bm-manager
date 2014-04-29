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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import com.google.common.io.Files;

/**
 * Concrete service implementation of {@link FileDataService} based on MongoDB.
 *
 * @author Derek Hulley
 * @since 1.4
 */
public class LocalTestFileService extends AbstractTestFileService
{
    private final String testFileDir;
    
    public LocalTestFileService(
            FileDataService fileDataService,
            String localDir,
            String testFileDir)
    {
        super(fileDataService, localDir);
        this.testFileDir = testFileDir;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("LocalTestFileService [testFileDir=").append(testFileDir);
        builder.append("]");
        return builder.toString();
    }

    /**
     * Uses 'local' plus the CRC32 of {@link #testFileDir}
     */
    @Override
    protected String getMirrorPath()
    {
        try
        {
            CRC32 crc = new CRC32();
            crc.update(testFileDir.getBytes("UTF-8"));
            return "local" + "/" + crc.getValue();
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("Failed to encode: " + testFileDir, e);
        }
    }

    /**
     * Does a listing of files on the local directory
     */
    @Override
    protected List<FileData> listRemoteFiles()
    {
        File testFileDir = new File(this.testFileDir);
        if (!testFileDir.exists())
        {
            throw new RuntimeException("Local directory not found: " + this);
        }
        File[] testFiles = testFileDir.listFiles();
        List<FileData> fileDatas = new ArrayList<FileData>(testFiles.length);
        for (File testFile : testFiles)
        {
            // Ignore directories
            if (testFile.isDirectory())
            {
                continue;
            }
            String testFilename = testFile.getName();
            String testFileExtension = FileData.getExtension(testFilename);
            long testFileSize = testFile.length();
            
            FileData fileData = new FileData();
            fileData.setRemoteName(testFilename);
            fileData.setExtension(testFileExtension);
            fileData.setSize(testFileSize);
            
            fileDatas.add(fileData);
        }
        // Done
        return fileDatas;
    }
    
    @Override
    protected void downloadRemoteFile(FileData fileData, File localFile) throws IOException
    {
        File testFileDir = new File(this.testFileDir);
        if (!testFileDir.exists())
        {
            throw new RuntimeException("Local directory not found: " + this);
        }
        File testFile = new File(testFileDir, fileData.getRemoteName());
        // Just copy it
        Files.copy(testFile, localFile);
    }
}
