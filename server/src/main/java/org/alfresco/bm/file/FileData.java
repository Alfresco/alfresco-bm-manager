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

import java.util.Locale;

/**
 * Data fields representing a file.
 * 
 * @author Derek Hulley
 * @since 1.4
 */
public class FileData
{
    public static final String FILESET_DEFAULT = "default";
    public static final String EXTENSION_BIN = "bin";
    
    private String fileset;
    private String remoteName;
    private String localName;
    private String extension;
    private long size;
    private String locale = Locale.US.toString();
    private String encoding = "UTF-8";
    private int randomizer;
    
    /**
     * Yet another method to extract the filename extension,
     * defaulting to {@link #EXTENSION_BIN 'bin'} if not found.
     */
    public static final String getExtension(String filename)
    {
        int index = filename.lastIndexOf(".");
        if (index <= 0)
        {
            return EXTENSION_BIN;
        }
        String ext = filename.substring(index+1); // avoid the '.'
        if (ext.length() == 0)
        {
            return EXTENSION_BIN;
        }
        else
        {
            return ext;
        }
    }
    
    public FileData()
    {
        randomizer = (int)(Math.random() * 1E6);
    }
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("FileData [fileset=");
        builder.append(fileset);
        builder.append(", remoteName=");
        builder.append(remoteName);
        builder.append(", localName=");
        builder.append(localName);
        builder.append(", extension=");
        builder.append(extension);
        builder.append(", size=");
        builder.append(size);
        builder.append(", locale=");
        builder.append(locale);
        builder.append(", encoding=");
        builder.append(encoding);
        builder.append(", randomizer=");
        builder.append(randomizer);
        builder.append("]");
        return builder.toString();
    }

    public String getFileset()
    {
        return fileset;
    }
    public void setFileset(String fileset)
    {
        this.fileset = fileset;
    }

    public String getRemoteName()
    {
        return remoteName;
    }
    public void setRemoteName(String remoteName)
    {
        this.remoteName = remoteName;
    }

    public String getLocalName()
    {
        return localName;
    }
    public void setLocalName(String localName)
    {
        this.localName = localName;
    }

    public String getExtension()
    {
        return extension;
    }
    public void setExtension(String extension)
    {
        this.extension = extension;
    }

    public long getSize()
    {
        return size;
    }
    public void setSize(long size)
    {
        this.size = size;
    }

    public String getLocale()
    {
        return locale;
    }
    public void setLocale(String locale)
    {
        this.locale = locale;
    }

    public String getEncoding()
    {
        return encoding;
    }
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public int getRandomizer()
    {
        return randomizer;
    }
    public void setRandomizer(int randomizer)
    {
        this.randomizer = randomizer;
    }
}
