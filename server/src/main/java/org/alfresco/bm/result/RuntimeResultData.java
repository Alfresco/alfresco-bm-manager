package org.alfresco.bm.result;

import org.alfresco.bm.exception.BenchmarkResultException;

/**
 * Runtime data result object
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class RuntimeResultData extends AbstractResultData
{
    /** Serialization ID */
    private static final long serialVersionUID = -8392430619747398391L;

    private long runtimeTicks;

    public RuntimeResultData(long runtime, String description)
            throws BenchmarkResultException
    {
        super(description);
        setRuntimeTicks(runtime);
    }

    public long getRuntimeTicks()
    {
        return runtimeTicks;
    }

    /**
     * Sets the runtime value
     * 
     * @param runtimeTicks
     *        ticks of runtime [ms]
     * 
     * @throws BenchmarkResultException
     *         if 'runtimeTicks' < 0
     */
    public void setRuntimeTicks(long runtimeTicks)
            throws BenchmarkResultException
    {
        if (runtimeTicks < 0)
        {
            throw new BenchmarkResultException(
                    "'runtimeTicks': positive value required!");
        }
        this.runtimeTicks = runtimeTicks;
    }

    /**
     * Returns combines runtime object
     * 
     * @param data1
     *        [RuntimeResultData]
     * @param data2
     *        [RuntimeResultData]
     * 
     * @return combines runtime object if descriptive value matches or exception
     * 
     * @throws BenchmarkResultException
     */
    public static RuntimeResultData Combine(RuntimeResultData data1,
            RuntimeResultData data2) throws BenchmarkResultException
    {
        if (null == data1 || null == data2)
        {
            throw new BenchmarkResultException("Data objects are mandatory!");
        }
        if (!data1.getDescription().equals(data2.getDescription()))
        {
            throw new BenchmarkResultException(
                    "Data objects must have the same descriptive type!");
        }

        long ticks = data1.getRuntimeTicks() + data2.getRuntimeTicks();
        return new RuntimeResultData(ticks, data1.getDescription());
    }

    @Override
    public String toJSON()
    {
        return "RuntimeResultData [runtimeTicks=" + this.runtimeTicks
                + ", description=" + getDescription() + "]";
    }
}
