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
package org.alfresco.bm.report;

/**
 * Report utility class that exports and formats data to a csv file.
 * 
 * @author Michael Suzuki
 * @since 1.0
 */
public class ReportUtil
{
    /**
     * Helper method to format collection of names into a csv format.
     * 
     * @param header collection of event names to use as labels for header
     * @return collection with a given order where first 2 labels are time and
     *         start separated by commas
     */
    public static String formatHeaders(final String names)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(names);
        sb.deleteCharAt(0);
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

}
