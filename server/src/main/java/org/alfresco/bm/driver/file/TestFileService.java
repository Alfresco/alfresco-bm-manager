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

import java.io.File;

/**
 * Provides access to test files
 *
 * @author Derek Hulley
 * @since 1.4
 */
public interface TestFileService
{
    /**
     * Find a specific file by name
     * 
     * @param filename      the name of the remote file to find
     * @return              a file or <tt>null</tt> if no test file with that name exists
     */
    File getFileByName(String filename);
    
    /**
     * Get a random file of any description
     * 
     * @return              a file or <tt>null</tt> if no test files exist
     */
    File getFile();
    
    /**
     * Get a random file with the given extension
     * 
     * @param extension     the file extension, which is no guarantee of mimetype
     * @return              a file or <tt>null</tt> if no such test file exists
     */
    File getFile(String extension);
}
