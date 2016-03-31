package org.alfresco.bm.result.data;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;
import org.bson.Document;

/**
 * Objects per second as benchmark result data
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public final class ObjectsPerSecondResultData extends AbstractResultData
{
    /** Serialization ID */
    private static final long serialVersionUID = 8396315103810117490L;

    public static final String FIELD_OBJECTS_PER_SECOND = "objPerSec";
    public static final String FIELD_OBJECT_TYPE = "objType";

    /** data type */
    public static final String DATA_TYPE = "OPS";

    double objectsPerSecond;
    ResultObjectType objectType;

    /** Constructor */
    public ObjectsPerSecondResultData(double objectsPerSecond,
            ResultObjectType objectType, Document description, ResultOperation resultOperation)
            throws BenchmarkResultException
    {
        super(description, resultOperation);
        setObjectType(objectType);
        setObjectsPerSecond(objectsPerSecond);
    }
    
    /** Constructor */
    public ObjectsPerSecondResultData(Document doc) throws BenchmarkResultException
    {
        super(doc);
        setObjectsPerSecond(doc.getDouble(FIELD_OBJECTS_PER_SECOND));
        setObjectType(ResultObjectType.valueOf(doc.getString(FIELD_OBJECT_TYPE)));
    }

    /**
     * @return number of objects per second
     */
    public double getObjectsPerSecond()
    {
        return this.objectsPerSecond;
    }

    /**
     * Sets the number of objects per second
     * 
     * @param value
     *        positive double value of objects
     */
    public void setObjectsPerSecond(double value)
            throws BenchmarkResultException
    {
        this.objectsPerSecond = value;
        if (this.objectsPerSecond < 0.0)
        {
            throw new BenchmarkResultException(
                    "'objectsPerSecond' must be a positive value.");
        }
    }

    /**
     * @return the affected object type
     */
    public ResultObjectType getObjectType()
    {
        return this.objectType;
    }

    /**
     * Sets the affected object type
     * 
     * @param type
     *        [ResultObjectType]
     */
    public void setObjectType(ResultObjectType type)
    {
        this.objectType = type;
    }

    /**
     * Combines to data objects to one
     * 
     * @param data1
     *        data object 1 to combine
     * @param data2
     *        data object 2 to combine
     * 
     * @return Combined object if object type and description is the same.
     * 
     * @throws BenchmarkResultException
     *         if object type or description differ
     */
    public static ObjectsPerSecondResultData combine(
            ObjectsPerSecondResultData data1, ObjectsPerSecondResultData data2)
            throws BenchmarkResultException
    {
        if (null == data1 || null == data2)
        {
            throw new BenchmarkResultException("Data objects are mandatory!");
        }
        if (!data1.getObjectType().equals(data2.getObjectType()))
        {
            throw new BenchmarkResultException(
                    "Data objects must have the same object type!");
        }
        if (null != data1.getDescription() && !data1.getDescription().equals(data2.getDescription()))
        {
            throw new BenchmarkResultException(
                    "Data objects must have the same descriptive type!");
        }
        if (data1.getResultOperation() != data2.getResultOperation())
        {
            throw new BenchmarkResultException(
                    "Data objects must have the same result operation type!");
        }

        double value = (data1.getObjectsPerSecond()
                + data2.getObjectsPerSecond()) / 2;
        return new ObjectsPerSecondResultData(value, data1.getObjectType(),
                data1.getDescription(), data1.getResultOperation());
    }

    @Override
    public Document toDocumentBSON()
    {
        Document doc = getDocumentBSON()
                .append(FIELD_OBJECTS_PER_SECOND, this.objectsPerSecond)
                .append(FIELD_OBJECT_TYPE, this.objectType.toString());

        return doc;
    }

    @Override
    public String getDataType()
    {
        return DATA_TYPE;
    }

    @Override
    protected void appendQueryParams(Document queryDoc)
    {
        queryDoc.append(FIELD_OBJECT_TYPE, this.objectType.toString());
    }
}
