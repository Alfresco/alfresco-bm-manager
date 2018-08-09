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
package org.alfresco.bm.manager.report;

import com.mongodb.DBCursor;

import java.util.List;

/**
 * extra data report service
 * 
 * Allows one or more extra data sheets for Excel results export.
 * 
 * @author Frank Becker
 * @since 2.0.10
 */
public interface DataReportService
{
    /**
     * Appends extra data for a test.run to a named sheet.
     * 
     * @param driverId
     *            (String, required) test driver ID
     * @param test
     *            (String, required) name of the test
     * @param run
     *            (String, required) name of the test run
     * @param sheetName
     *            (String, required) name of the extra data sheet
     * @param fieldNames
     *            (String [], required) field names of the values to write
     * @param values
     *            (String [], required) data to write to the extra data sheet
     * 
     *            Note: {@see fieldNames} and {@see values} must have the same array size.
     */
    void appendData(String driverId, String test, String run, String sheetName, String[] fieldNames, String[] values);

    /**
     * Removes all data from the tables affecting to driver (if provided), test and test run (if provided).
     * 
     * @param driverId
     *            (String, optional) Driver ID
     * @param test
     *            (String, required) test name
     * @param testRun
     *            (String, optional) test run name
     */
    void remove(String driverId, String test, String testRun);

    /**
     * Updates the column names for the extra data for a test.run named sheet.
     * 
     * @param driverId
     *            (String, required) test driver ID
     * @param test
     *            (String, required) name of the test
     * @param run
     *            (String, required) name of the test run
     * @param sheetName
     *            (String, required) name of the extra data sheet
     * @param fieldNames
     *            (String [], required) field names of the values to write
     * @param description
     *            (String [], required) data to write to the extra data sheet
     * 
     *            Note: {@see fieldNames} and {@see values} must have the same array size.
     */
    void setDescription(String driverId, String test, String run, String sheetName, String[] fieldNames,
            String[] description);

    /**
     * Gets the column headers / description for a test.run data sheet
     * 
     * @param driverId
     *            (String, optional) test driver ID
     * @param test
     *            (String, required) name of the test
     * @param run
     *            (String, required) name of the test run
     * @param sheetName
     *            (String, required) name of the extra data sheet
     * 
     * @return (String []) - may be null if no description was set
     */
    List<String> getDescription(String driverId, String test, String run, String sheetName);

    /**
     * Gets the extra data sheet names for a specific test.run
     * 
     * @param driverId
     *            (String, optional) test driver ID
     * @param test
     *            (String, required) name of the test
     * @param run
     *            (String, required) name of the test run
     * 
     * @return (String []) - may be null if no extra data was stored for the test.run
     */
    String[] getSheetNames(String driverId, String test, String run);

    /**
     * Returns a DB cursor for a test.run extra data sheet
     * 
     * @param driverId
     *            (String, optional) test driver ID
     * @param test
     *            (String, required) name of the test
     * @param run
     *            (String, required) name of the test run
     * @param sheetName
     *            (String, required) name of the extra data sheet
     * 
     * @return (DBCursor) - filter rows for value fields with {@see isValueField}
     */
    DBCursor getData(String driverId, String test, String run, String sheetName);

    /**
     * checks if the field name is a user defined value field or a value field
     * 
     * @param fieldName
     *            (String, required) field name to validate
     * 
     * @return true if not one of the predefined fixed field names, false else
     */
    boolean isValueField(String fieldName);

    /**
     * Returns the next row with values or null if no more values.
     * 
     * @param cursor
     *            (DBCursor) cursor
     * 
     * @return (String []) or null
     */
    List<String> getNextValueRow(DBCursor cursor);
}
