/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
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
package org.alfresco.bm.common;

import java.util.Date;

/**
 * POJO representing test run scheduling
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class TestRunSchedule
{
    private int version;
    private long scheduled;

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("TestRunSchedule ");
        builder.append("[version=").append(version);
        builder.append(", scheduled=").append(new Date(scheduled));
        builder.append("]");
        return builder.toString();
    }
    public int getVersion()
    {
        return version;
    }
    public void setVersion(int version)
    {
        this.version = version;
    }
    public long getScheduled()
    {
        return scheduled;
    }
    public void setScheduled(long scheduled)
    {
        this.scheduled = scheduled;
    }
}
