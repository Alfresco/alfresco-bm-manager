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
package org.alfresco.bm.integration.test.model;

import java.util.List;

/**
 * POJO representing a test run summary
 * 
 * @author Andrei Forascu
 * @since 3.0
 */

public class TestRunSummary
{
    private Object _id;
    private Object test;
    private String name;
    private Integer version;
    private String description;
    private String state;
    private Long scheduled;
    private Long started;
    private Long stopped;
    private Long completed;
    private Integer duration;
    private Integer progress;
    private Integer resultsSuccess;
    private Integer resultsFail;
    private Integer resultsTotal;
    private Double successRate;
    private List<String> drivers;

    public Object get_id()
    {
        return _id;
    }

    public void set_id(Object _id)
    {
        this._id = _id;
    }

    public Object getTest()
    {
        return test;
    }

    public void setTest(Object test)
    {
        this.test = test;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public Long getScheduled()
    {
        return scheduled;
    }

    public void setScheduled(Long scheduled)
    {
        this.scheduled = scheduled;
    }

    public Long getStarted()
    {
        return started;
    }

    public void setStarted(Long started)
    {
        this.started = started;
    }

    public Long getStopped()
    {
        return stopped;
    }

    public void setStopped(Long stopped)
    {
        this.stopped = stopped;
    }

    public Long getCompleted()
    {
        return completed;
    }

    public void setCompleted(Long completed)
    {
        this.completed = completed;
    }

    public Integer getDuration()
    {
        return duration;
    }

    public void setDuration(Integer duration)
    {
        this.duration = duration;
    }

    public Integer getProgress()
    {
        return progress;
    }

    public void setProgress(Integer progress)
    {
        this.progress = progress;
    }

    public Integer getResultsSuccess()
    {
        return resultsSuccess;
    }

    public void setResultsSuccess(Integer resultsSuccess)
    {
        this.resultsSuccess = resultsSuccess;
    }

    public Integer getResultsFail()
    {
        return resultsFail;
    }

    public void setResultsFail(Integer resultsFail)
    {
        this.resultsFail = resultsFail;
    }

    public Integer getResultsTotal()
    {
        return resultsTotal;
    }

    public void setResultsTotal(Integer resultsTotal)
    {
        this.resultsTotal = resultsTotal;
    }

    public Double getSuccessRate()
    {
        return successRate;
    }

    public void setSuccessRate(Double successRate)
    {
        this.successRate = successRate;
    }

    public List<String> getDrivers()
    {
        return drivers;
    }

    public void setDrivers(List<String> drivers)
    {
        this.drivers = drivers;
    }
}
