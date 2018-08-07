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

/**
 * Provides crud against mirror data for test files.
 * <p/>
 * Note that this does not actually manipulate files but just manages metadata for the files.
 *
 * @author Derek Hulley
 * @since 1.4
 */
public interface FileDataService
{
    /**
     * Insert new data about a file
     */
    void createNewFileData(FileData fileData);
    
    /**
     * Get the number of files in a given fileset
     */
    long fileCount(String fileset);
    
    /**
     * Find a specific file based on the file name in the remote store
     * 
     * @param fileset       the name of the fileset
     * @param remoteName    the name of the file in the remote store
     * @return              the file data or <tt>null</tt> if it does not exist
     */
    FileData findFile(String fileset, String remoteName);
    
    /**
     * Remove a specific file index based on the file name in the remote store
     * 
     * @param fileset       the name of the fileset
     * @param remoteName    the name of the file in the remote store
     */
    void removeFile(String fileset, String remoteName);
    
    /**
     * Get a random file description from a given dataset
     * 
     * @param fileset       the name of a dataset to choose a file from
     * @return              a file or <tt>null</tt> if there isn't one
     */
    FileData getRandomFile(String fileset);
    
    /**
     * Get a random file description from a given dataset and with the given file extension
     * 
     * @param fileset       the name of a dataset to choose a file from
     * @param extension     the extension e.g. '<code>txt<code>'
     * @return              a file or <tt>null</tt> if there isn't one
     */
    FileData getRandomFile(String fileset, String extension);
}
