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

import org.apache.commons.io.FilenameUtils;

/**
 * Data representing folder
 * 
 * @author Derek Hulley
 * @since 4.0.4
 */
public class FolderData
{
    private final String id;
    private final String root;
    private final String path;
    private final long folderCount;
    private final long fileCount;

    /**
     * @param id                the unique ID of the folder.  The value must be unique but is dependent on the format used by the system in test
     * @param root              a root folder that the path is relative to.  The format is determined by the target test system.
     * @param path              the folder patch from the root.  The <tt>root</tt> and <tt>path</tt> combination must be unique
     * @param folderCount       the number of subfolders in the folder
     * @param fileCount         the number of files in the folder
     * 
     * @throws IllegalArgumentException if the values are <tt>null</tt>
     */
    public FolderData(String id, String root, String path, long folderCount, long fileCount)
    {
        if (id == null || root == null || path == null)
        {
            throw new IllegalArgumentException();
        }
        
        this.id = id;
        this.root = root;
        this.path = path;
        this.fileCount = fileCount;
        this.folderCount = folderCount;
    }

    @Override
    public String toString()
    {
        return "FolderData [id=" + id + ", root=" + root + ", path=" + path + ", folderCount=" + folderCount + ", fileCount=" + fileCount + "]";
    }

    @Override
    public int hashCode()
    {
        return (root.hashCode() + 31 * path.hashCode());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FolderData other = (FolderData) obj;
        
        return
                this.folderCount == other.folderCount &&
                this.fileCount == other.fileCount &&
                this.id.equals(other.id) &&
                this.root.equals(other.root) &&
                this.path.equals(other.path);
    }

    public String getId()
    {
        return id;
    }

    public String getRoot()
    {
        return root;
    }

    public String getPath()
    {
        return path;
    }

    public long getFolderCount()
    {
        return folderCount;
    }
    
    public long getFileCount()
    {
        return fileCount;
    }
    
    /**
     * @see FilenameUtils#getName(String)
     */
    public String getName()
    {
        return FilenameUtils.getName(path);
    }
    
    /**
     * Get the path that the parent would have.
     * 
     * @return      the parent path i.e. the path without the {@link #getName() name} of this folder
     */
    public String getParentPath()
    {
        return FilenameUtils.getFullPathNoEndSeparator(path);
    }
}
