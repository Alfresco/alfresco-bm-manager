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
