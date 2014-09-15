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
import java.util.Map;
import java.util.TreeMap;

import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.test.TestRunServicesCache;
import org.alfresco.bm.test.TestService;
import org.alfresco.bm.test.TestService.NotFoundException;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.poi.POIXMLProperties.CoreProperties;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mongodb.DBObject;

/**
 * Generates XLSX report for all events, including summary results, failures, etc.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class XLSXReporter extends AbstractEventReporter
{
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
    }
    
    private void writeHeaderAndFooter(XSSFSheet sheet) throws IOException, NotFoundException
    {
        // Title
        sheet.getFooter().setCenter(title);
    }
    
    private void createSummarySheet(XSSFWorkbook workbook) throws IOException, NotFoundException
    {
        TestService testService = getTestService();
        ResultService resultService = getResultService();
        DBObject testRunObj = getTestService().getTestRunMetadata(test, run);

        // Create the sheet
        XSSFSheet sheet = workbook.createSheet("Summary");
        writeHeaderAndFooter(sheet);
        
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
    }
}
