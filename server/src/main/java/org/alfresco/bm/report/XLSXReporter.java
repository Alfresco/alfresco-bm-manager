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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.alfresco.bm.api.AbstractRestResource;
import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.event.ResultService.ResultHandler;
import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.ResultDataService;
import org.alfresco.bm.result.data.ObjectsPerSecondResultData;
import org.alfresco.bm.result.data.ObjectsResultData;
import org.alfresco.bm.result.data.ResultData;
import org.alfresco.bm.result.data.RuntimeResultData;
import org.alfresco.bm.result.defs.ResultDBDataFields;
import org.alfresco.bm.result.defs.ResultOperation;
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
import org.apache.poi.ss.usermodel.Chart;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.charts.AxisPosition;
import org.apache.poi.ss.usermodel.charts.ChartAxis;
import org.apache.poi.ss.usermodel.charts.ChartDataSource;
import org.apache.poi.ss.usermodel.charts.ChartLegend;
import org.apache.poi.ss.usermodel.charts.DataSources;
import org.apache.poi.ss.usermodel.charts.LegendPosition;
import org.apache.poi.ss.usermodel.charts.LineChartData;
import org.apache.poi.ss.usermodel.charts.LineChartSeries;
import org.apache.poi.ss.usermodel.charts.ValueAxis;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;

import com.mongodb.BasicDBList;
import com.mongodb.DBCursor;
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
            try
            {
                os.close();
            }
            catch (Throwable e)
            {
            }
        }
    }

    private void writeToWorkbook(XSSFWorkbook workbook) throws IOException, NotFoundException
    {
        writeMetadata(workbook);
        createSummarySheet(workbook);
        createPropertiesSheet(workbook);
        createEventSheets(workbook);
        createExtraDataSheet(workbook);
        createResultDataSheet(workbook);
    }

    /**
     * Creates the sheet with the result data from the
     * {@see org.alfresco.bm.result.ResultDataService}
     * 
     * @param workbook
     */
    private void createResultDataSheet(XSSFWorkbook workbook)
    {
        // get the service
        ResultDataService resultDataService = this.services.getTestDAO().getResultDataService();
        if (null == resultDataService)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("No 'ResultDataService' available - no result sheet will be generated ...");
            }
            return;
        }

        // get benchmark ID
        String bmId = null;
        try
        {
            bmId = this.services.getTestDAO().getBenchmarkID(this.test);
        }
        catch (BenchmarkResultException e)
        {
            logger.error("Unable to get the Benchmark ID for test '" + test + "'.", e);
            return;
        }

        // create the query document
        Document queryDoc = new Document(ResultDBDataFields.FIELD_BM_ID, bmId)
                .append(ResultDBDataFields.FIELD_TEST_NAME, this.test)
                .append(ResultDBDataFields.FIELD_TEST_RUN_NAME, this.run);

        List<Document> results = null;
        try
        {
            results = resultDataService.queryDocuments(queryDoc);
        }
        catch (BenchmarkResultException e)
        {
            logger.error("Unable to get benchmark results for test '" + test + "', run '" + run + "' with BM_ID '"
                    + bmId + "'.", e);
            return;
        }
        
        // Create the sheet
        XSSFSheet sheet = workbook.createSheet("Result Data");
     
        // Create the fonts we need
        Font fontBold = workbook.createFont();
        fontBold.setBoldweight(Font.BOLDWEIGHT_BOLD);

        // Create the styles we need
        XSSFCellStyle rightDataStyle = sheet.getWorkbook().createCellStyle();
        rightDataStyle.setAlignment(HorizontalAlignment.RIGHT);
        XSSFCellStyle leftDataStyle = sheet.getWorkbook().createCellStyle();
        rightDataStyle.setAlignment(HorizontalAlignment.LEFT);
        XSSFCellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        headerStyle.setAlignment(HorizontalAlignment.RIGHT);
        headerStyle.setFont(fontBold);
        
        XSSFRow row = null;
        int rowCount = 0;

        for (final Document doc : results)
        {
            String headline = getResultDataHeadline(doc);
            
            row = sheet.createRow(rowCount++);
            {
                // headline
                row.getCell(0).setCellValue(headline);
                row.getCell(0).setCellStyle(headerStyle);

                String dataType = doc.getString(ResultDBDataFields.FIELD_DATA_TYPE);
                switch (dataType)
                {
                    case RuntimeResultData.DATA_TYPE:
                        long runticks = doc.getLong(ResultDBDataFields.FIELD_RUN_TICKS);
                        row.getCell(1).setCellValue((double)runticks);
                        row.getCell(1).setCellStyle(rightDataStyle);
                        row.getCell(2).setCellValue("ms");
                        row.getCell(2).setCellStyle(leftDataStyle);
                        break;

                    case ObjectsPerSecondResultData.DATA_TYPE:
                        double objectsPerSecond = doc.getDouble(ResultDBDataFields.FIELD_OBJECTS_PER_SECOND);
                        row.getCell(1).setCellValue(objectsPerSecond);
                        row.getCell(1).setCellStyle(rightDataStyle);
                        break;

                    case ObjectsResultData.DATA_TYPE:
                        long number = doc.getLong(ResultDBDataFields.FIELD_NUMBER_OF_OBJECTS);
                        row.getCell(1).setCellValue((double)number);
                        row.getCell(1).setCellStyle(rightDataStyle);
                        break;

                    default:
                        logger.error("Unknown data type: " + dataType);
                }
            }
        }
        // Auto-size the columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);

        // Printing
        PrintSetup ps = sheet.getPrintSetup();
        sheet.setAutobreaks(true);
        ps.setFitWidth((short) 1);
        ps.setLandscape(false);
    }

    private String getResultDataHeadline(Document doc)
    {
        String headline = "";

        String type = doc.getString(ResultDBDataFields.FIELD_DATA_TYPE);
        String resOp = doc.getString(ResultDBDataFields.FIELD_RESULT_OP);
        String objectType = "";
        if (type.equals(RuntimeResultData.DATA_TYPE))
        {
            if (ResultOperation.None.toString().equals(resOp))
            {
                resOp = "Overall";
            }
            headline = "Runtime (" + resOp + "):";
        }
        else
        {
            objectType = doc.getString(ResultDBDataFields.FIELD_OBJECT_TYPE);
            if (type.equals(ObjectsPerSecondResultData.DATA_TYPE))
            {
                headline = objectType + "(s)/sec, " + resOp + ":"; 
            }
            else if (type.equals(ObjectsResultData.DATA_TYPE))
            {
                headline = "Number of " + objectType + "(s), " + resOp + ":";
            }
            else 
            {
                headline = "Unknown data type: " + type;
            }
        }
        return headline;
    }
    
    /**
     * Creates the sheet(s) with extra data from the {@see org.alfresco.bm.report.DataReportService}
     * 
     * @param workbook
     *            (XSSFWorkbook) Excel workbook
     * 
     * @since 2.0.10
     */
    private void createExtraDataSheet(XSSFWorkbook workbook)
    {
        // get the service
        DataReportService dataReportService = this.services.getDataReportService(this.test, this.run);
        if (null == dataReportService)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("No 'DataReportService' available - no 'extra data' sheet will be generated ...");
            }
            return;
        }

        // get the sheet names from the report service
        String[] sheets = dataReportService.getSheetNames(null, this.test, this.run);
        if (null == sheets || 0 == sheets.length)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("No sheets to write - quitting ...");
            }
            return;
        }

        // create the sheets
        for (String sheet : sheets)
        {
            if (null != sheet && !sheet.isEmpty())
            {
                createExtraDataSheet(workbook, dataReportService, sheet);
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Skipping sheet with no name - check data written to MongoDB!");
                }
            }
        }
    }

    /**
     * Creates a new named sheet and inserts the data for it.
     * 
     * @param workbook
     *            (XSSFWorkbook) Excel workbook to create the sheet in
     * @param dataReportService
     *            (DataReportService) provides the extra data to write to the sheet
     * @param sheetName
     *            (String) name of the new sheet
     * 
     * @since 2.0.10
     */
    private void createExtraDataSheet(XSSFWorkbook workbook, DataReportService dataReportService, String sheetName)
    {
        XSSFSheetRow sheet = new XSSFSheetRow();
        int columnCount = 0;
        
        // get description - if not empty write to sheet
        List<String> descriptionRow = dataReportService.getDescription(null, this.test, this.run, sheetName);
        if (null != descriptionRow && descriptionRow.size() > 0)
        {
            createSheetRow(workbook, sheet, sheetName, descriptionRow, true);
        }

        // get the data for the sheet
        DBCursor cursor = dataReportService.getData(null, this.test, this.run, sheetName);
        if (null != cursor)
        {
            // for each row create entry in the sheet
            while (cursor.hasNext())
            {
                List<String> values = dataReportService.getNextValueRow(cursor);
                createSheetRow(workbook, sheet, sheetName, values, false);
                
                // set number of columns
                if (null != values && values.size() > columnCount)
                {
                    columnCount = values.size();
                }
            }
        }
        
        // auto-size the columns
        if (null != sheet.sheet)
        {
         // Auto-size the columns
            for (int i = 0; i < columnCount; i++)
            {
                sheet.sheet.autoSizeColumn(i);
            }
        }
    }

    /**
     * Helper class to create rows in a XLSX sheet
     * 
     * @since 2.0.10
     */
    private class XSSFSheetRow
    {
        public XSSFSheet sheet = null;
        public int rowCount = 0;
    }

    /**
     * Creates a new line with values in the sheet.
     * 
     * @param workbook
     *            (XSSFWorkbook, required) workbook to create the row in
     * @param sheetRow
     *            (XSSFSheetRow, required) sheet to create the data row in
     * @param sheetName
     *            (String, required) name of the sheet
     * @param values
     *            (String [], optional) if null or empty no work will be done, else the values written to the next line
     * @param bold
     *            (boolean) true: the values will be set in bold font face, else normal
     * 
     * @since 2.0.10
     */
    private void createSheetRow(XSSFWorkbook workbook, XSSFSheetRow sheetRow, String sheetName, List<String> values,
            boolean bold)
    {
        if (null != values && values.size() > 0)
        {
            // check if sheet exists and create if not
            if (null == sheetRow.sheet)
            {
                sheetRow.sheet = workbook.createSheet(sheetName);
            }

            // create cell style
            XSSFCellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.CENTER);

            if (bold)
            {
                // Create bold font
                Font fontBold = workbook.createFont();
                fontBold.setBoldweight(Font.BOLDWEIGHT_BOLD);
                cellStyle.setFont(fontBold);
            }

            // create row
            XSSFRow row = sheetRow.sheet.createRow(sheetRow.rowCount++);

            // set values
            for (int i = 0; i < values.size(); i++)
            {
                row.getCell(i).setCellValue(values.get(i));
                row.getCell(i).setCellStyle(cellStyle);
            }
        }
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
                row.getCell(1).setCellValue(
                        FastDateFormat.getDateTimeInstance(FastDateFormat.MEDIUM, FastDateFormat.MEDIUM).format(time));
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
                row.getCell(1).setCellValue(
                        FastDateFormat.getDateTimeInstance(FastDateFormat.MEDIUM, FastDateFormat.MEDIUM).format(time));
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
        row = sheet.createRow(rowCount++); // Header row
        String[] headers = new String[] { "Event Name", "Total Count", "Success Count", "Failure Count",
                "Success Rate (%)", "Min (ms)", "Max (ms)", "Arithmetic Mean (ms)", "Standard Deviation (ms)" };
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
            row.getCell(columnCount++).setCellValue((long) statsSuccess.getMin());
            // Max (ms)
            row.getCell(columnCount++).setCellValue((long) statsSuccess.getMax());
            // Arithmetic Mean (ms)
            row.getCell(columnCount++).setCellValue((long) statsSuccess.getMean());
            // Standard Deviation (ms)
            row.getCell(columnCount++).setCellValue((long) statsSuccess.getStandardDeviation());
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
        ps.setFitWidth((short) 1);
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
        ps.setFitWidth((short) 1);
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
        long windowSize = AbstractEventReporter.getWindowSize(start, end, 100); // Well-known window sizes

        // Keep track of sheets by event name. Note that XLSX truncates sheets to 31 chars, so use 28 chars and ~01, ~02
        final Map<String, String> sheetNames = new HashMap<String, String>(31);
        final Map<String, XSSFSheet> sheets = new HashMap<String, XSSFSheet>(31);
        final Map<String, AtomicInteger> rowNums = new HashMap<String, AtomicInteger>(31);

        ResultHandler handler = new ResultHandler()
        {
            @Override
            public boolean processResult(long fromTime, long toTime,
                    Map<String, DescriptiveStatistics> statsByEventName, Map<String, Integer> failuresByEventName)
                    throws Throwable
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
                            sheet.getPrintSetup().setFitWidth((short) 1);
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

                    double numPerSec = (double) stats.getN() / ((double) (toTime - fromTime) / 1000.0);
                    double failuresPerSec = (double) failures / ((double) (toTime - fromTime) / 1000.0);

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

        // Create charts in the sheets
        for (String eventName : sheetNames.keySet())
        {
            // Get the sheet name
            String sheetName = sheetNames.get(eventName);
            if (sheetName == null)
            {
                logger.error("Did not find sheet for event: " + eventName);
                continue;
            }
            // Get the sheet
            XSSFSheet sheet = sheets.get(sheetName);
            if (sheet == null)
            {
                logger.error("Did not find sheet for name: " + sheetName);
                continue;
            }
            // What row did we get up to
            AtomicInteger rowNum = rowNums.get(eventName);
            if (rowNum == null)
            {
                logger.error("Did not find row number for event: " + sheetName);
                continue;
            }

            // This axis is common to both charts
            ChartDataSource<Number> xTime = DataSources.fromNumericCellRange(sheet,
                    new CellRangeAddress(1, rowNum.intValue() - 1, 0, 0));

            // Graph of event times
            XSSFDrawing drawingTimes = sheet.createDrawingPatriarch();
            ClientAnchor anchorTimes = drawingTimes.createAnchor(0, 0, 0, 0, 0, 5, 15, 25);
            Chart chartTimes = drawingTimes.createChart(anchorTimes);
            ChartLegend legendTimes = chartTimes.getOrCreateLegend();
            legendTimes.setPosition(LegendPosition.BOTTOM);

            LineChartData chartDataTimes = chartTimes.getChartDataFactory().createLineChartData();

            ChartAxis bottomAxisTimes = chartTimes.getChartAxisFactory().createCategoryAxis(AxisPosition.BOTTOM);
            bottomAxisTimes.setNumberFormat("#,##0;-#,##0");
            ValueAxis leftAxisTimes = chartTimes.getChartAxisFactory().createValueAxis(AxisPosition.LEFT);

            // Mean
            ChartDataSource<Number> yMean = DataSources.fromNumericCellRange(sheet,
                    new CellRangeAddress(1, rowNum.intValue() - 1, 1, 1));
            LineChartSeries yMeanSerie = chartDataTimes.addSeries(xTime, yMean);
            yMeanSerie.setTitle(title + " - " + eventName + ": Mean (ms)");

            // Std Dev
            ChartDataSource<Number> yStdDev = DataSources.fromNumericCellRange(sheet,
                    new CellRangeAddress(1, rowNum.intValue() - 1, 4, 4));
            LineChartSeries yStdDevSerie = chartDataTimes.addSeries(xTime, yStdDev);
            yStdDevSerie.setTitle(title + " - " + eventName + ": Standard Deviation (ms)");

            // Plot event times
            chartTimes.plot(chartDataTimes, bottomAxisTimes, leftAxisTimes);

            // Graph of event volumes

            // Graph of event times
            XSSFDrawing drawingVolumes = sheet.createDrawingPatriarch();
            ClientAnchor anchorVolumes = drawingVolumes.createAnchor(0, 0, 0, 0, 0, 25, 15, 35);
            Chart chartVolumes = drawingVolumes.createChart(anchorVolumes);
            ChartLegend legendVolumes = chartVolumes.getOrCreateLegend();
            legendVolumes.setPosition(LegendPosition.BOTTOM);

            LineChartData chartDataVolumes = chartVolumes.getChartDataFactory().createLineChartData();

            ChartAxis bottomAxisVolumes = chartVolumes.getChartAxisFactory().createCategoryAxis(AxisPosition.BOTTOM);
            bottomAxisVolumes.setNumberFormat("#,##0;-#,##0");
            ValueAxis leftAxisVolumes = chartVolumes.getChartAxisFactory().createValueAxis(AxisPosition.LEFT);

            // Number per second
            ChartDataSource<Number> yNumPerSec = DataSources.fromNumericCellRange(sheet,
                    new CellRangeAddress(1, rowNum.intValue() - 1, 6, 6));
            LineChartSeries yNumPerSecSerie = chartDataVolumes.addSeries(xTime, yNumPerSec);
            yNumPerSecSerie.setTitle(title + " - " + eventName + ": Events per Second");

            // Failures per second
            ChartDataSource<Number> yFailPerSec = DataSources.fromNumericCellRange(sheet, new CellRangeAddress(1,
                    rowNum.intValue() - 1, 8, 8));
            LineChartSeries yFailPerSecSerie = chartDataVolumes.addSeries(xTime, yFailPerSec);
            yFailPerSecSerie.setTitle(title + " - " + eventName + ": Failures per Second");

            // Plot volumes
            chartVolumes.plot(chartDataVolumes, bottomAxisVolumes, leftAxisVolumes);
        }
    }
}
