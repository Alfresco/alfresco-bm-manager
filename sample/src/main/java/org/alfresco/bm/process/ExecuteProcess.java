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
package org.alfresco.bm.process;

import java.io.File;
import java.util.Collections;

import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.data.ProcessData;
import org.alfresco.bm.data.ProcessDataDAO;
import org.alfresco.bm.event.AbstractEventProcessor;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventResult;
import org.alfresco.bm.file.TestFileService;

/**
 * Execute an unfinished process
 * 
 * <h1>Input</h1>
 * 
 * Process name
 * 
 * <h1>Data</h1>
 * 
 * A MongoDB collection containing unprocessed data.
 * 
 * <h1>Actions</h1>
 * 
 * Fetches the process from the MongoDB collection and executes it.
 * There is no real processing for this sample, but the basic principle of
 * running a process from associated data, marking it as done and recording
 * the result is demonstrated.<br/>
 * A certain number of failures are demostrated as well.
 * 
 * <h1>Output</h1>
 * 
 * {@link #EVENT_NAME_PROCESS_DONE}: The process name<br/>
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class ExecuteProcess extends AbstractEventProcessor
{
    public static final String EVENT_NAME_PROCESS_DONE = "processDone";
    
    private final ProcessDataDAO processDataDAO;
    private final TestFileService testFileService;
    private String eventNameProcessDone;

    /**
     * @param processDataDAO        general DAO for accessing data
     */
    public ExecuteProcess (ProcessDataDAO processDataDAO, TestFileService testFileService)
    {
        super();
        this.processDataDAO = processDataDAO;
        this.testFileService = testFileService;
        this.eventNameProcessDone = EVENT_NAME_PROCESS_DONE;
    }

    /**
     * Override the {@link #EVENT_NAME_PROCESS_DONE default} event name for 'process done'.
     */
    public void setEventNameProcessDone(String eventNameProcessDone)
    {
        this.eventNameProcessDone = eventNameProcessDone;
    }

    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        // Usually, the entire method is timed but we can choose to control this
        super.suspendTimer();
        
        // Get the user email
        String processName = (String) event.getDataObject();
        
        // Locate the process
        ProcessData process = processDataDAO.findProcessByName(processName);
        // A quick double-check
        EventResult result = null;
        if (process == null)
        {
            result = new EventResult(
                    "Skipping processing for '" + processName + "'.  Process not found.",
                    false);
            return result;
        }
        else if (process.getState() != DataCreationState.Scheduled)
        {
            result = new EventResult(
                    "Skipping processing for '" + processName + "'.  Process not scheduled.",
                    false);
            return result;
        }
        
        // Get and check that we have access to the required test file
        File file = testFileService.getFile();
        if (file == null || !file.exists())
        {
            result = new EventResult(
                    "Skipping processing for '" + processName + "'.  No test file available.",
                    Collections.<Event>emptyList(),
                    false);
            return result;
        }
        
        // Restart the clock
        resumeTimer();
        
        // Simulate some process delay
        synchronized (processName)
        {
            try
            {
                processName.wait((long)(Math.random()* 50.0) + 10L);
            }
            catch (InterruptedException e)
            {
                // Ignore
            }
        }
        
        /*
         * Some work
         */
        
        switch ((int)(Math.random() * 10.0))
        {
            // 10% exceptions
            case 0:
                // This would normally be done in a try-catch
                processDataDAO.updateProcessState(processName, DataCreationState.Failed);
                // We let the framework handle this
                throw new RuntimeException("A ficticious random exception during execution of '" + processName + "'.");
            // 20% some error condition
            case 1:
            case 2:
                processDataDAO.updateProcessState(processName, DataCreationState.Failed);
                result = new EventResult(
                        "A ficticious random error response for executing process '" + processName + "'.",
                        Collections.<Event>emptyList(),
                        false);
                break;
            // 70% success
            default:
                // Record the name of the process to reflect that is was created on the mythical server
                boolean updated = processDataDAO.updateProcessState(processName, DataCreationState.Created);
                if (updated)
                {
                    // Create 'done' event, which will not have any further associated event processors
                    Event doneEvent = new Event(eventNameProcessDone, 0L, processName);
                    result = new EventResult("Process " + processName + " completed.", doneEvent);
                    break;
                }
                else
                {
                    // This is a real error condition i.e. not a simulated error
                    throw new RuntimeException("Process " + processName + " was executed but not recorded.");
                }
        }
        // Done
        return result;
    }
}
