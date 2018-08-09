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
package org.alfresco.bm.cm;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Data representing folder
 * 
 * @author Derek Hulley
 * @since 4.0.4
 */
public class FolderData
{
    private final String id;
    private final String context;
    private final String path;
    private final int level;
    private final String name;
    private final String parentPath;
    private final long folderCount;
    private final long fileCount;

    /**
     * @param id                the unique ID of the folder.  The value must be unique but is dependent on the format used by the system in test
     * @param context           an arbitrary context in which the folder path is valid.  The format is determined by the target test system.
     * @param path              the folder path with the given context.  The <tt>context</tt> and <tt>path</tt> combination must be unique
     * @param folderCount       the number of subfolders in the folder
     * @param fileCount         the number of files in the folder
     * 
     * @throws IllegalArgumentException if the values are <tt>null</tt>
     */
    public FolderData(String id, String context, String path, long folderCount, long fileCount)
    {
        if (id == null || context == null || path == null)
        {
            throw new IllegalArgumentException();
        }
        if (!path.isEmpty() && (!path.startsWith("/") || path.endsWith("/")))
        {
            throw new IllegalArgumentException("Path must start with '/' and not end with '/'.");
        }
        
        this.id = id;
        this.context = context;
        this.path = path;
        this.fileCount = fileCount;
        this.folderCount = folderCount;
        
        // Derived data
        this.level = StringUtils.countMatches(path,  "/");
        this.name = FilenameUtils.getName(path);
        this.parentPath = FilenameUtils.getFullPathNoEndSeparator(path);
    }

    @Override
    public String toString()
    {
        return "FolderData [id=" + id + ", context=" + context + ", path=" + path + ", folderCount=" + folderCount + ", fileCount=" + fileCount + "]";
    }

    @Override
    public int hashCode()
    {
        return (context.hashCode() + 31 * path.hashCode());
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
                this.context.equals(other.context) &&
                this.path.equals(other.path);
    }

    public String getId()
    {
        return id;
    }

    public String getContext()
    {
        return context;
    }

    public String getPath()
    {
        return path;
    }
    
    public int getLevel()
    {
        return level;
    }
    
    /**
     * @see FilenameUtils#getName(String)
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Get the path that the parent would have.
     * 
     * @return      the parent path i.e. the path without the {@link #getName() name} of this folder
     */
    public String getParentPath()
    {
        return parentPath;
    }

    public long getFolderCount()
    {
        return folderCount;
    }
    
    public long getFileCount()
    {
        return fileCount;
    }
}
