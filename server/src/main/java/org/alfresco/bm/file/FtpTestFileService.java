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
package org.alfresco.bm.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * FTP-based implementation of {@link AbstractTestFileService}.
 *
 * @author Derek Hulley
 * @since 1.4
 */
public class FtpTestFileService extends AbstractTestFileService
{
    private static Log logger = LogFactory.getLog(FtpTestFileService.class);
    
    private final String ftpHost;
    private final int ftpPort;
    private final String ftpUsername;
    private final String ftpPassword;
    private final String ftpPath;
    private boolean ftpLocalPassiveMode = true;
    
    public FtpTestFileService(
            FileDataService fileDataService,
            String localDir,
            String ftpHost,
            int ftpPort,
            String ftpUsername,
            String ftpPassword,
            String ftpPath)
    {
        super(fileDataService, localDir);
        this.ftpHost = ftpHost;
        this.ftpPort = ftpPort;
        this.ftpUsername = ftpUsername;
        this.ftpPassword = ftpPassword;
        this.ftpPath = ftpPath;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("FtpTestFileService [ftpHost=").append(ftpHost);
        builder.append(", ftpPort=").append(ftpPort);
        builder.append(", ftpUsername=").append(ftpUsername);
        builder.append(", ftpPassword=").append("*****");
        builder.append(", ftpPath=").append(ftpPath);
        builder.append("]");
        return builder.toString();
    }

    /**
     * Force the FTP client to {@link FTPClient#enterLocalPassiveMode() enter local passive mode}.
     * This is useful where the server does not have visibility of the client.
     * 
     * @param ftpLocalPassiveMode           <tt>true</tt> to enter local passive mode
     */
    public void setFtpLocalPassiveMode(boolean ftpLocalPassiveMode)
    {
        this.ftpLocalPassiveMode = ftpLocalPassiveMode;
    }

    /**
     * Provides a safe (connected) FTP client
     */
    private FTPClient getFTPClient()
    {
        FTPClient ftp;
        try
        {
            // Connect to the FTP server
            ftp = new FTPClient();
            // Connect
            ftp.connect(ftpHost, ftpPort);
            if (!ftp.login(ftpUsername, ftpPassword))
            {
                throw new IOException("FTP credentials rejected.");
            }
            if (ftpLocalPassiveMode)
            {
                ftp.enterLocalPassiveMode();
            }
            // Settings for the FTP channel
            ftp.setControlKeepAliveTimeout(300);
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.setAutodetectUTF8(false);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply))
            {
                throw new IOException("FTP server refused connection.");
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("FTP communication failed: " + this, e);
        }
        // Done
        return ftp;
    }
    
    /**
     * Combines the {@link #ftpHost} and {@link #ftpPath} into a relative path.
     */
    @Override
    protected String getMirrorPath()
    {
        return ftpHost + "/" + ftpPath;
    }

    /**
     * Does a listing of files on the FTP server
     */
    @Override
    protected List<FileData> listRemoteFiles()
    {
        // Get a list of files from the FTP server
        FTPClient ftp = getFTPClient();
        FTPFile[] ftpFiles = new FTPFile[0];
        try
        {
            if (!ftp.changeWorkingDirectory(ftpPath))
            {
                throw new IOException("Failed to change directory (leading '/' could be a problem): " + ftpPath);
            }
            ftpFiles = ftp.listFiles();
        }
        catch (IOException e)
        {
            throw new RuntimeException("FTP file listing failed: " + this, e);
        }
        finally
        {
            try
            {
                ftp.logout();
                ftp.disconnect();
            }
            catch (IOException e)
            {
                logger.warn("Failed to close FTP connection: " + e.getMessage());
            }
        }
        // Index each of the files
        List<FileData> remoteFileDatas = new ArrayList<FileData>(ftpFiles.length);
        for (FTPFile ftpFile : ftpFiles)
        {
            String ftpFilename = ftpFile.getName();
            // Watch out for . and ..
            if (ftpFilename.equals(".") || ftpFilename.equals(".."))
            {
                continue;
            }
            String ftpExtension = FileData.getExtension(ftpFilename);
            long ftpSize = ftpFile.getSize();
            
            FileData remoteFileData = new FileData();
            remoteFileData.setRemoteName(ftpFilename);
            remoteFileData.setExtension(ftpExtension);
            remoteFileData.setSize(ftpSize);
            
            remoteFileDatas.add(remoteFileData);
        }
        // Done
        return remoteFileDatas;
    }
    
    @Override
    protected void downloadRemoteFile(FileData fileData, File localFile) throws IOException
    {
        String remoteName = ftpPath + "/" + fileData.getRemoteName();
        FTPClient ftp = null;
        OutputStream os = null;
        try
        {
            os = new BufferedOutputStream(new FileOutputStream(localFile));
            // It does not exist locally, so go and retrieve it
            ftp = getFTPClient();
            boolean success = ftp.retrieveFile(remoteName, os);
            if (!success)
            {
                throw new IOException("Failed to complete download of file: " + fileData + " by " + this);
            }
        }
        finally
        {
            if (os != null)
            {
                try { os.close(); } catch (Throwable e) {}
            }
            try
            {
                ftp.logout();
                ftp.disconnect();
            }
            catch (IOException e)
            {
                logger.warn("Failed to close FTP connection: " + e.getMessage());
            }
        }
    }
}
