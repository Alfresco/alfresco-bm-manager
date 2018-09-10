package org.alfresco.bm.integration.test.model;

import java.util.List;

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
    private Integer successRate;
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

    public Integer getSuccessRate()
    {
        return successRate;
    }

    public void setSuccessRate(Integer successRate)
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
