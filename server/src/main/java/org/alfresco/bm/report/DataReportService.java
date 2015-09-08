package org.alfresco.bm.report;

import com.mongodb.DBCursor;

/**
 * extra data report service
 * 
 * Allows one or more extra data sheets for Excel results export.
 * 
 * @author Frank Becker
 * @since 2.10
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
     *            (String, required) test driver ID
     * @param test
     *            (String, required) name of the test
     * @param run
     *            (String, required) name of the test run
     * @param sheetName
     *            (String, required) name of the extra data sheet
     * 
     * @return (String []) - may be null if no description was set
     */
    String[] getDescription(String driverId, String test, String run, String sheetName);

    /**
     * Gets the extra data sheet names for a specific test.run
     * 
     * @param driverId
     *            (String, required) test driver ID
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
     *            (String, required) test driver ID
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
    String[] getNextValueRow(DBCursor cursor);
}
