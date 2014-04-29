package org.alfresco.bm.file;

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
