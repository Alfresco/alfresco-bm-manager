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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.alfresco.bm.api.AbstractRestResource;
import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.event.ResultService.ResultHandler;
import org.alfresco.bm.test.TestRunServicesCache;
import org.alfresco.bm.test.TestService;
import org.alfresco.bm.test.TestService.NotFoundException;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.poi.POIXMLProperties.CoreProperties;
import org.apache.poi.openxml4j.util.Nullable;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;

/**
 * Generates XLSX report for all events, including summary results, failures, etc.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class XLSXReporter extends AbstractEventReporter
{
    private static Log logger = LogFactory.getLog(XLSXReporter.class);
    
    private final String title;
    
    public XLSXReporter(TestRunServicesCache services, String test, String run)
    {
        super(services, test, run);
        title = test + "." + run;
    }
    
    @Override
    public void export(OutputStream os)
    {
        XSSFWorkbook workbook = new XSSFWorkbook();
        // Set defaults
        workbook.setMissingCellPolicy(Row.CREATE_NULL_AS_BLANK);
        
        // Write to the workbook
        try
        {
            writeToWorkbook(workbook);
            workbook.write(os);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to write workbook: " + this, e);
        }
        finally
        {
            try { os.close(); } catch (Throwable e) {}
        }
    }
    
    private void writeToWorkbook(XSSFWorkbook workbook) throws IOException, NotFoundException
    {
        writeMetadata(workbook);
        createSummarySheet(workbook);
        createPropertiesSheet(workbook);
        createEventSheets(workbook);
    }
    
    private void writeMetadata(XSSFWorkbook workbook) throws IOException, NotFoundException
    {
        TestService testService = getTestService();
        
        CoreProperties workbookCoreProperties = workbook.getProperties().getCoreProperties();
        
        // Title
        workbookCoreProperties.setTitle(title);
        
        // Description
        StringBuilder description = new StringBuilder(128);
        DBObject testObj = testService.getTestMetadata(test);
        String testDescription = (String) testObj.get(FIELD_DESCRIPTION);
        if (testDescription != null)
        {
            description.append(testDescription).append("\n");
        }
        DBObject testRunObj = testService.getTestRunMetadata(test, run);
        String testRunDescription = (String) testRunObj.get(FIELD_DESCRIPTION);
        if (testRunDescription != null)
        {
            description.append(testRunDescription);
        }
        workbookCoreProperties.setDescription(description.toString().trim());
        
        // Created
        Long time = (Long) testRunObj.get(FIELD_STARTED);
        if (time > 0)
        {
            workbookCoreProperties.setCreated(new Nullable<Date>(new Date(time)));
        }
    }
    
    /**
     * Create a 'Summary' sheet containing the table of averages
     */
    private void createSummarySheet(XSSFWorkbook workbook) throws IOException, NotFoundException
    {
        DBObject testRunObj = getTestService().getTestRunMetadata(test, run);

        // Create the sheet
        XSSFSheet sheet = workbook.createSheet("Summary");
        
        // Create the fonts we need
        Font fontBold = workbook.createFont();
        fontBold.setBoldweight(Font.BOLDWEIGHT_BOLD);
        
        // Create the styles we need
        XSSFCellStyle summaryDataStyle = sheet.getWorkbook().createCellStyle();
        summaryDataStyle.setAlignment(HorizontalAlignment.RIGHT);
        XSSFCellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        headerStyle.setAlignment(HorizontalAlignment.RIGHT);
        headerStyle.setFont(fontBold);
        
        XSSFRow row = null;
        int rowCount = 0;
        row = sheet.createRow(rowCount++);
        {
            row.getCell(0).setCellValue("Name:");
            row.getCell(0).setCellStyle(headerStyle);
            row.getCell(1).setCellValue(title);
            row.getCell(1).setCellStyle(summaryDataStyle);
        }
        row = sheet.createRow(rowCount++);
        {
            String description = (String) testRunObj.get(FIELD_DESCRIPTION);
            description = description == null ? "" : description;
            row.getCell(0).setCellValue("Description:");
            row.getCell(0).setCellStyle(headerStyle);
            row.getCell(1).setCellValue(description);
            row.getCell(1).setCellStyle(summaryDataStyle);
        }
        row = sheet.createRow(rowCount++);
        {
            row.getCell(0).setCellValue("Progress (%):");
            row.getCell(0).setCellStyle(headerStyle);
            Double progress = (Double) testRunObj.get(FIELD_PROGRESS);
            progress = progress == null ? 0.0 : progress;
            row.getCell(1).setCellValue(progress * 100);
            row.getCell(1).setCellType(XSSFCell.CELL_TYPE_NUMERIC);
            row.getCell(1).setCellStyle(summaryDataStyle);
        }
        row = sheet.createRow(rowCount++);
        {
            row.getCell(0).setCellValue("State:");
            row.getCell(0).setCellStyle(headerStyle);
            String state = (String) testRunObj.get(FIELD_STATE);
            if (state != null)
            {
                row.getCell(1).setCellValue(state);
                row.getCell(1).setCellStyle(summaryDataStyle);
            }
        }
        row = sheet.createRow(rowCount++);
        {
            row.getCell(0).setCellValue("Started:");
            row.getCell(0).setCellStyle(headerStyle);
            Long time = (Long) testRunObj.get(FIELD_STARTED);
            if (time > 0)
            {
                row.getCell(1).setCellValue(FastDateFormat.getDateTimeInstance(FastDateFormat.MEDIUM, FastDateFormat.MEDIUM).format(time));
                row.getCell(1).setCellStyle(summaryDataStyle);
            }
        }
        row = sheet.createRow(rowCount++);
        {
            row.getCell(0).setCellValue("Finished:");
            row.getCell(0).setCellStyle(headerStyle);
            Long time = (Long) testRunObj.get(FIELD_COMPLETED);
            if (time > 0)
            {
                row.getCell(1).setCellValue(FastDateFormat.getDateTimeInstance(FastDateFormat.MEDIUM, FastDateFormat.MEDIUM).format(time));
                row.getCell(1).setCellStyle(summaryDataStyle);
            }
        }
        row = sheet.createRow(rowCount++);
        {
            row.getCell(0).setCellValue("Duration:");
            row.getCell(0).setCellStyle(headerStyle);
            Long time = (Long) testRunObj.get(FIELD_DURATION);
            if (time > 0)
            {
                row.getCell(1).setCellValue(DurationFormatUtils.formatDurationHMS(time));
                row.getCell(1).setCellStyle(summaryDataStyle);
            }
        }
        
        rowCount++;
        rowCount++;
        // Create a header row
        row = sheet.createRow(rowCount++);          // Header row
        String[] headers = new String[]
                {
                    "Event Name", "Total Count", "Success Count", "Failure Count", "Success Rate (%)", "Min (ms)", "Max (ms)", "Arithmetic Mean (ms)", "Standard Deviation (ms)"
                };
        int columnCount = 0;
        for (String header : headers)
        {
            XSSFCell cell = row.getCell(columnCount++);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(header);
        }
        // Grab results and output them
        columnCount = 0;
        TreeMap<String, ResultSummary> summaries = collateResults(true);
        for (Map.Entry<String, ResultSummary> entry : summaries.entrySet())
        {
            // Reset column count
            columnCount = 0;

            row = sheet.createRow(rowCount++);
            String eventName = entry.getKey();
            ResultSummary summary = entry.getValue();
            SummaryStatistics statsSuccess = summary.getStats(true);
            SummaryStatistics statsFail = summary.getStats(false);
            // Event Name
            row.getCell(columnCount++).setCellValue(eventName);
            // Total Count
            row.getCell(columnCount++).setCellValue(summary.getTotalResults());
            // Success Count
            row.getCell(columnCount++).setCellValue(statsSuccess.getN());
            // Failure Count
            row.getCell(columnCount++).setCellValue(statsFail.getN());
            // Success Rate (%)
            row.getCell(columnCount++).setCellValue(summary.getSuccessPercentage());
            // Min (ms)
            row.getCell(columnCount++).setCellValue((long)statsSuccess.getMin());
            // Max (ms)
            row.getCell(columnCount++).setCellValue((long)statsSuccess.getMax());
            // Arithmetic Mean (ms)
            row.getCell(columnCount++).setCellValue((long)statsSuccess.getMean());
            // Standard Deviation (ms)
            row.getCell(columnCount++).setCellValue((long)statsSuccess.getStandardDeviation());
        }
        
        // Auto-size the columns
        for (int i = 0; i < 10; i++)
        {
            sheet.autoSizeColumn(i);
        }
        sheet.setColumnWidth(1, 5120);
        
        // Printing
        PrintSetup ps = sheet.getPrintSetup();
        sheet.setAutobreaks(true);
        ps.setFitWidth((short)1);
        ps.setLandscape(true);

        // Header and footer
        sheet.getHeader().setCenter(title);
    }
    
    private void createPropertiesSheet(XSSFWorkbook workbook) throws IOException, NotFoundException
    {
        DBObject testRunObj = services.getTestDAO().getTestRun(test, run, true);
        if (testRunObj == null)
        {
            return;
        }
        // Ensure we don't leak passwords
        testRunObj = AbstractRestResource.maskValues(testRunObj);
        
        BasicDBList propertiesList = (BasicDBList) testRunObj.get(FIELD_PROPERTIES);
        if (propertiesList == null)
        {
            return;
        }
        // Order the properties, nicely
        TreeMap<String, DBObject> properties = new TreeMap<String, DBObject>();
        for (Object propertyObj : propertiesList)
        {
            DBObject property = (DBObject) propertyObj;
            String key = (String) property.get(FIELD_NAME);
            properties.put(key, property);
        }
        
        XSSFSheet sheet = workbook.createSheet("Properties");
        
        // Create the fonts we need
        Font fontBold = workbook.createFont();
        fontBold.setBoldweight(Font.BOLDWEIGHT_BOLD);
        
        // Create the styles we need
        XSSFCellStyle propertyStyle = sheet.getWorkbook().createCellStyle();
        propertyStyle.setAlignment(HorizontalAlignment.RIGHT);
        propertyStyle.setWrapText(true);
        XSSFCellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        headerStyle.setAlignment(HorizontalAlignment.RIGHT);
        headerStyle.setFont(fontBold);

        XSSFRow row = null;
        int rowCount = 0;
        XSSFCell cell = null;
        int cellCount = 0;
        row = sheet.createRow(rowCount++);
        cell = row.createCell(cellCount++);
        {
            cell.setCellValue("Property");
            cell.setCellStyle(headerStyle);
        }
        cell = row.createCell(cellCount++);
        {
            cell.setCellValue("Value");
            cell.setCellStyle(headerStyle);
        }
        cell = row.createCell(cellCount++);
        {
            cell.setCellValue("Origin");
            cell.setCellStyle(headerStyle);
        }
        cellCount = 0;
        
        // Iterate all the properties for the test run
        for (Map.Entry<String, DBObject> entry : properties.entrySet())
        {
            DBObject property = entry.getValue();
            String key = (String) property.get(FIELD_NAME);
            String value = (String) property.get(FIELD_VALUE);
            String origin = (String) property.get(FIELD_ORIGIN);
            
            row = sheet.createRow(rowCount++);
            cell = row.createCell(cellCount++);
            {
                cell.setCellValue(key);
                cell.setCellStyle(propertyStyle);
            }
            cell = row.createCell(cellCount++);
            {
                cell.setCellValue(value);
                cell.setCellStyle(propertyStyle);
            }
            cell = row.createCell(cellCount++);
            {
                cell.setCellValue(origin);
                cell.setCellStyle(propertyStyle);
            }
            // Back to first column
            cellCount = 0;
        }
        
        // Size the columns
        sheet.autoSizeColumn(0);
        sheet.setColumnWidth(1, 15360);
        sheet.autoSizeColumn(2);

        // Printing
        PrintSetup ps = sheet.getPrintSetup();
        sheet.setAutobreaks(true);
        ps.setFitWidth((short)1);
        ps.setLandscape(true);

        // Header and footer
        sheet.getHeader().setCenter(title);
    }
    
    private void createEventSheets(final XSSFWorkbook workbook)
    {
        // Create the fonts we need
        Font fontBold = workbook.createFont();
        fontBold.setBoldweight(Font.BOLDWEIGHT_BOLD);
        
        // Create the styles we need
        CreationHelper helper = workbook.getCreationHelper();
        final XSSFCellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.RIGHT);
        final XSSFCellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setAlignment(HorizontalAlignment.RIGHT);
        headerStyle.setFont(fontBold);
        final XSSFCellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(helper.createDataFormat().getFormat("HH:mm:ss"));
        
        // Calculate a good window size
        ResultService resultService = getResultService();
        EventRecord firstResult = resultService.getFirstResult();
        EventRecord lastResult = resultService.getLastResult();
        if (firstResult == null || lastResult == null)
        {
            return;
        }
        long start = firstResult.getStartTime();
        long end = lastResult.getStartTime();
        long windowSize = AbstractEventReporter.getWindowSize(start, end, 100);         // Well-known window sizes
        
        // Keep track of sheets by event name.  Note that XLSX truncates sheets to 31 chars, so use 28 chars and ~01, ~02
        final Map<String, String> sheetNames = new HashMap<String, String>(31);
        final Map<String, XSSFSheet> sheets = new HashMap<String, XSSFSheet>(31);
        final Map<String, AtomicInteger> rowNums = new HashMap<String, AtomicInteger>(31);
        
        ResultHandler handler = new ResultHandler()
        {
            @Override
            public boolean processResult(
                    long fromTime, long toTime,
                    Map<String, DescriptiveStatistics> statsByEventName,
                    Map<String, Integer> failuresByEventName) throws Throwable
            {
                // Get or create a sheet for each event
                for (String eventName : statsByEventName.keySet())
                {
                    // What sheet name to we use?
                    String sheetName = sheetNames.get(eventName);
                    if (sheetName == null)
                    {
                        sheetName = eventName;
                        if (eventName.length() > 28)
                        {
                            int counter = 1;
                            // Find a sheet name not in use
                            while (true)
                            {
                                sheetName = eventName.substring(0, 28);
                                sheetName = String.format("%s~%02d", sheetName, counter);
                                // Have we used this, yet?
                                if (sheets.containsKey(sheetName))
                                {
                                    // Yes, we have used it.
                                    counter++;
                                    continue;
                                }
                                // This is unique
                                break;
                            }
                        }
                        sheetNames.put(eventName, sheetName);
                    }
                    // Get and create the sheet, if necessary
                    XSSFSheet sheet = sheets.get(sheetName);
                    if (sheet == null)
                    {
                        // Create
                        try
                        {
                            sheet = workbook.createSheet(sheetName);
                            sheets.put(sheetName, sheet);
                            sheet.getHeader().setCenter(title + " - " + eventName);
                            sheet.getPrintSetup().setFitWidth((short)1);
                            sheet.getPrintSetup().setLandscape(true);
                        }
                        catch (Exception e)
                        {
                            logger.error("Unable to create workbook sheet for event: " + eventName, e);
                            continue;
                        }
                        // Intro
                        XSSFCell cell = sheet.createRow(0).createCell(0);
                        cell.setCellValue(title + " - " + eventName + ":");
                        cell.setCellStyle(headerStyle);
                        // Headings
                        XSSFRow row = sheet.createRow(1);
                        cell = row.createCell(0);
                        cell.setCellStyle(headerStyle);
                        cell.setCellValue("time");
                        cell = row.createCell(1);
                        cell.setCellStyle(headerStyle);
                        cell.setCellValue("mean");
                        cell = row.createCell(2);
                        cell.setCellStyle(headerStyle);
                        cell.setCellValue("min");
                        cell = row.createCell(3);
                        cell.setCellStyle(headerStyle);
                        cell.setCellValue("max");
                        cell = row.createCell(4);
                        cell.setCellStyle(headerStyle);
                        cell.setCellValue("stdDev");
                        cell = row.createCell(5);
                        cell.setCellStyle(headerStyle);
                        cell.setCellValue("num");
                        cell = row.createCell(6);
                        cell.setCellStyle(headerStyle);
                        cell.setCellValue("numPerSec");
                        cell = row.createCell(7);
                        cell.setCellStyle(headerStyle);
                        cell.setCellValue("fail");
                        cell = row.createCell(8);
                        cell.setCellStyle(headerStyle);
                        cell.setCellValue("failPerSec");
                        // Size the columns
                        sheet.autoSizeColumn(0);
                        sheet.autoSizeColumn(1);
                        sheet.autoSizeColumn(2);
                        sheet.autoSizeColumn(3);
                        sheet.autoSizeColumn(4);
                        sheet.autoSizeColumn(5);
                        sheet.autoSizeColumn(6);
                        sheet.autoSizeColumn(7);
                        sheet.autoSizeColumn(8);
                    }
                    AtomicInteger rowNum = rowNums.get(eventName);
                    if (rowNum == null)
                    {
                        rowNum = new AtomicInteger(2);
                        rowNums.put(eventName, rowNum);
                    }
                    
                    DescriptiveStatistics stats = statsByEventName.get(eventName);
                    Integer failures = failuresByEventName.get(eventName);
                    
                    double numPerSec = (double) stats.getN() / ( (double) (toTime-fromTime) / 1000.0);
                    double failuresPerSec = (double) failures / ( (double) (toTime-fromTime) / 1000.0);
                    
                    XSSFRow row = sheet.createRow(rowNum.getAndIncrement());
                    XSSFCell cell;
                    cell = row.createCell(0, Cell.CELL_TYPE_NUMERIC);
                    cell.setCellStyle(dateStyle);
                    cell.setCellValue(new Date(toTime));
                    cell = row.createCell(5, Cell.CELL_TYPE_NUMERIC);
                    cell.setCellValue(stats.getN());
                    cell = row.createCell(6, Cell.CELL_TYPE_NUMERIC);
                    cell.setCellValue(numPerSec);
                    cell = row.createCell(7, Cell.CELL_TYPE_NUMERIC);
                    cell.setCellValue(failures);
                    cell = row.createCell(8, Cell.CELL_TYPE_NUMERIC);
                    cell.setCellValue(failuresPerSec);
                    // Leave out values if there is no mean
                    if (Double.isNaN(stats.getMean()))
                    {
                        continue;
                    }
                    cell = row.createCell(1, Cell.CELL_TYPE_NUMERIC);
                    cell.setCellValue(stats.getMean());
                    cell = row.createCell(2, Cell.CELL_TYPE_NUMERIC);
                    cell.setCellValue(stats.getMin());
                    cell = row.createCell(3, Cell.CELL_TYPE_NUMERIC);
                    cell.setCellValue(stats.getMax());
                    cell = row.createCell(4, Cell.CELL_TYPE_NUMERIC);
                    cell.setCellValue(stats.getStandardDeviation());
                }
                return true;
            }
        };
        resultService.getResults(handler, start, windowSize, windowSize, false);
    }
}
