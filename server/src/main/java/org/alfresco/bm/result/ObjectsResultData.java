package org.alfresco.bm.result;

import org.alfresco.bm.exception.BenchmarkResultException;

/**
 * Number of objects result data object
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class ObjectsResultData extends AbstractResultData
{
    /** Serialization ID */
    private static final long serialVersionUID = -6089478547147621395L;

    private long numberOfObjects;
    private ResultObjectType objectType;

    /**
     * Constructor
     * 
     * @param count
     *        number of objects
     * @param objectType
     *        type of affected objects
     * @param description
     *        descriptive field (JSON)
     */
    public ObjectsResultData(long count, ResultObjectType objectType,
            String description) throws BenchmarkResultException
    {
        super(description);
        setObjectType(objectType);
        setNumberOfObjects(count);
    }

    /**
     * @return Number off objects affected
     */
    public long getNumberOfObjects()
    {
        return numberOfObjects;
    }

    /**
     * Sets the number of objects
     * 
     * @param numberOfObjects
     *        number of objects affected
     * 
     * @throws BenchmarkResultException
     *         if 'numberOfObjects' < 0
     */
    public void setNumberOfObjects(long numberOfObjects)
            throws BenchmarkResultException
    {
        if (numberOfObjects < 0)
        {
            throw new BenchmarkResultException(
                    "'numberOfObjects': positive value required!");
        }
        this.numberOfObjects = numberOfObjects;
    }

    /**
     * @return [ResultObjectType]
     */
    public ResultObjectType getObjectType()
    {
        return objectType;
    }

    /**
     * Sets the affected object type
     * 
     * @param objectType
     *        [ResultObjectType]
     */
    public void setObjectType(ResultObjectType objectType)
    {
        this.objectType = objectType;
    }

    /**
     * Combines two result data objects
     * 
     * @param data1
     *        [ObjectsResultData]
     * @param data2
     *        [ObjectsResultData]
     * 
     * @return
     *         total number of objects if descriptive value and object type are
     *         the same, exception else
     * 
     * @throws BenchmarkResultException
     */
    public static ObjectsResultData Combine(ObjectsResultData data1,
            ObjectsResultData data2) throws BenchmarkResultException
    {
        if (null == data2 || null == data2)
        {
            throw new BenchmarkResultException("Data objects are mandatory!");
        }
        if (!data1.getObjectType().equals(data2.getObjectType()))
        {
            throw new BenchmarkResultException("Data object types must match!");
        }
        if (!data1.getDescription().equals(data2.getDescription()))
        {
            throw new BenchmarkResultException(
                    "Data objects must have the same descriptive type!");
        }
        long count = data1.getNumberOfObjects() + data2.getNumberOfObjects();
        return new ObjectsResultData(count, data1.getObjectType(),
                data1.getDescription());
    }

    @Override
    public String toJSON()
    {
        return "ObjectsResultData [numberOfObjects=" + this.numberOfObjects
                + ", objectType=" + this.objectType
                + ", description=" + getDescription() + "]";
    }
}
