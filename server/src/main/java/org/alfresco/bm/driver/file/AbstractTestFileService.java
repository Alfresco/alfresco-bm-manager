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
package org.alfresco.bm.driver.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Abstract service implementation of {@link FileDataService} based on MongoDB.
 *
 * @author Derek Hulley
 * @since 1.4
 */
public abstract class AbstractTestFileService implements TestFileService, InitializingBean
{
    private static final String PROPERTIES_FILE = "TestFileService.properties";
    private static final String PROPERTY_FILESET = "fileset";

    private static Log logger = LogFactory.getLog(AbstractTestFileService.class);

    private final FileDataService fileDataService;
    private final String localDir;

    private File mirrorDir;
    private String fileset;

    public AbstractTestFileService(FileDataService fileDataService, String localDir)
    {
        this.fileDataService = fileDataService;
        this.localDir = localDir;
    }

    @Override
    public void afterPropertiesSet()
    {
        // TODO: FTP needs access to remote FTP server. This needs to be reimplemented
        //indexFileData();
    }

    /**
     * Get the data mirror path (relative to the root location).  This is implementation-specific
     * as it depends on the way in which data is stored in the remote location.
     *
     * @return a relative path in which local files will be mirrored
     */
    protected abstract String getMirrorPath();

    /**
     * Provides a safe method to get the directory where physical files are mirrored.
     * This method is static and synchronized as access to the mirror directory
     * is VM-wide
     */
    private static synchronized File initMirrorDir(String localDir, String mirrorPath)
    {
        // Check that the local directory exists
        File localStorage = new File(localDir);
        if (localStorage.exists() && !localStorage.isDirectory())
        {
            throw new RuntimeException("Local directory property ('localDir') is not a valid directory: " + localStorage);
        }
        // Attempt to make the storage
        if (!localStorage.exists())
        {
            localStorage.mkdirs();
        }
        if (!localStorage.exists() || !localStorage.isDirectory())
        {
            throw new RuntimeException("Failed to create local storage folder: " + localStorage);
        }
        // Create the local directory to mirror the server path
        File mirrorFolder = new File(localDir, mirrorPath);
        try
        {
            mirrorFolder.mkdirs();
        }
        catch (Throwable e)
        {
            throw new RuntimeException("Failed to create mirror folder: " + mirrorFolder, e);
        }
        // Done
        return mirrorFolder;
    }

    /**
     * Looks for a {@link #PROPERTIES_FILE properties file} containing the name of the fileset that
     * this server uses.  The fileset is therefore unique to every local data location.
     */
    private static synchronized String getFileset(File mirrorDir)
    {
        Properties properties = new Properties();
        // See if there is a file with the properties present
        File propsFile = new File(mirrorDir, PROPERTIES_FILE);
        if (propsFile.exists())
        {
            if (propsFile.isDirectory())
            {
                throw new RuntimeException("Expected to find a properties file but found a directory: " + propsFile);
            }
            // Just read the server's unique key from it
            properties = new Properties();
            FileReader reader = null;
            try
            {
                reader = new FileReader(propsFile);
                properties.load(reader);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to load properties from file: " + propsFile, e);
            }
            finally
            {
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (IOException e)
                    {
                    }
                }
            }
        }

        // Read the property
        String fileset = properties.getProperty(PROPERTY_FILESET);
        if (fileset == null)
        {
            // We must write a value into the file
            fileset = UUID.randomUUID().toString();
            properties.put(PROPERTY_FILESET, fileset);
            // Write the properties back
            FileWriter writer = null;
            try
            {
                writer = new FileWriter(propsFile);
                properties.store(writer, "Auto-generated fileset name");
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to write fileset to properties file: " + propsFile, e);
            }
            finally
            {
                if (writer != null)
                {
                    try
                    {
                        writer.close();
                    }
                    catch (IOException e)
                    {
                    }
                }
            }
        }
        // Done
        return fileset;
    }

    /**
     * List all files on the remote server
     *
     * @return a list of data with the {@link FileData#getRemoteName() remote name} populated
     */
    protected abstract List<FileData> listRemoteFiles();

    /**
     * Initialize the service by indexing the files on the FTP server
     */
    protected void indexFileData()
    {
        if (mirrorDir != null)
        {
            //consider this as initialized
            return;
        }
        // Get the local path (implementation-specific)
        String mirrorPath = getMirrorPath();
        // Make sure that the mirror directory is present
        mirrorDir = initMirrorDir(localDir, mirrorPath);

        // Get the fileset
        fileset = getFileset(mirrorDir);

        // Get a listing of the files
        List<FileData> remoteFileDatas = listRemoteFiles();
        if (remoteFileDatas.size() == 0)
        {
            throw new RuntimeException("No remote tests files: " + this);
        }

        // Index each of the files
        for (FileData remoteFileData : remoteFileDatas)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Processing details of remote file: " + remoteFileData);
            }

            String remoteName = remoteFileData.getRemoteName();
            String extension = FileData.getExtension(remoteName);
            long remoteSize = remoteFileData.getSize();

            // Check if the file is already present in the index
            FileData fileData = fileDataService.findFile(fileset, remoteName);
            String localName = null;
            if (fileData != null)
            {
                localName = fileData.getLocalName();
                // Check that the sizes match
                if (fileData.getSize() != remoteSize)
                {
                    // Size difference, so remove file index
                    fileDataService.removeFile(fileset, remoteName);
                    // and remove local file
                    File localFile = new File(mirrorDir, localName);
                    if (localFile.exists())
                    {
                        localFile.delete();
                    }
                }
                // Check that the local file, if it exists, is of the correct size
                File localFile = new File(mirrorDir, localName);
                if (localFile.exists() && localFile.length() != fileData.getSize())
                {
                    // Local file is incorrect
                    localFile.delete();
                }
            }
            else
            {
                localName = UUID.randomUUID().toString() + "." + extension;

                fileData = new FileData();
                fileData.setFileset(fileset);
                fileData.setRemoteName(remoteName);
                fileData.setLocalName(localName);
                fileData.setExtension(extension);
                fileData.setSize(remoteSize);
                // Create the index data
                fileDataService.createNewFileData(fileData);
            }
        }
        // Done
    }

    /**
     * Download the file represented from the remote location to the local file.
     * Note that all stream closures must be handled internally but IO errors
     * can be allowed out.
     *
     * @param fileData  data containing details of the remote file
     * @param localFile the local file to write to
     * @throws IOException will be handled by the calling code
     */
    protected abstract void downloadRemoteFile(FileData fileData, File localFile) throws IOException;

    @Override
    public File getFileByName(String filename)
    {
        indexFileData();
        FileData fileData = fileDataService.findFile(fileset, filename);
        return getFile(fileData);
    }

    @Override
    public File getFile()
    {
        indexFileData();
        FileData fileData = fileDataService.getRandomFile(fileset);
        return getFile(fileData);
    }

    @Override
    public File getFile(String extension)
    {
        indexFileData();
        FileData fileData = fileDataService.getRandomFile(fileset, extension);
        return getFile(fileData);
    }

    /**
     * Resolve the given file data into a real file
     */
    private File getFile(FileData fileData)
    {
        if (fileData == null)
        {
            return null;
        }
        indexFileData();
        // We have some data.
        // Do we already have it locally?
        File localFile = new File(mirrorDir, fileData.getLocalName());
        // Download the file, if required
        if (!localFile.exists())
        {
            try
            {
                downloadRemoteFile(fileData, localFile);
            }
            catch (Exception e)
            {
                // Unable to get the remote file
                String remoteName = fileData.getRemoteName();
                fileDataService.removeFile(fileset, remoteName);
                try
                {
                    localFile.delete();
                }
                catch (Exception ee)
                {
                }
                throw new RuntimeException("Failed to download file from remote server: " + this, e);
            }
        }
        // Done
        return localFile;
    }

    /**
     * Test file name for JUnit testing
     *
     * @since 2.1.1
     */
    private String testFileName = null;

    /**
     * Sets test file name for JUnit testing
     *
     * @since 2.1.1
     */
    public void setTestFileName(String testFileName)
    {
        this.testFileName = testFileName;
    }

    /**
     * @return (String) Test file name for JUnit testing
     * @since 2.1.1
     */
    public String getTestFileName()
    {
        return this.testFileName;
    }
}
