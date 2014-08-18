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

import org.alfresco.bm.test.TestRunServicesCache;
import org.alfresco.bm.test.TestService;
import org.alfresco.bm.test.TestService.NotFoundException;
import org.apache.poi.POIXMLProperties.CoreProperties;
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
    public XLSXReporter(TestRunServicesCache services, String test, String run)
    {
        super(services, test, run);
    }
    
    @Override
    public void export(OutputStream os)
    {
        XSSFWorkbook workbook = new XSSFWorkbook();
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
    }
    
    private void writeMetadata(XSSFWorkbook workbook) throws IOException, NotFoundException
    {
        TestService testService = getTestService();
        
        CoreProperties workbookCoreProperties = workbook.getProperties().getCoreProperties();
        
        // Title
        String title = test + "." + run;
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
}
