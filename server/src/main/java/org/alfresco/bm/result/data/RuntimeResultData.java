package org.alfresco.bm.result.data;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.defs.ResultOperation;
import org.bson.Document;

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

    public RuntimeResultData(long runtime, Document description, ResultOperation resultOperation)
            throws BenchmarkResultException
    {
        super(description, resultOperation);
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
    public static RuntimeResultData combine(RuntimeResultData data1,
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
        if (data1.getResultOperation() != data2.getResultOperation())
		{
			throw new BenchmarkResultException(
					"Data objects must have the same result operation type!");
		}

        long ticks = data1.getRuntimeTicks() + data2.getRuntimeTicks();
        return new RuntimeResultData(ticks, data1.getDescription(), data1.getResultOperation());
    }

    @Override
    public Document toDocumentBSON()
    {
        Document doc = getDocumentBSON()
                .append("runtimeTicks", this.runtimeTicks);

        return doc;
    }
}
