package org.alfresco.bm.result.data;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;
import org.bson.Document;

/**
 * Number of objects result data object
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public final class ObjectsResultData extends AbstractResultData
{
    /** Serialization ID */
    private static final long serialVersionUID = -6089478547147621395L;

    /** data type */
    public static final String DATA_TYPE = "OBJ";

    /** field names */
    public static final String FIELD_OBJECT_TYPE = "objType";
    public static final String FIELD_NUMBER_OF_OBJECTS = "numObj";

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
            Document description, ResultOperation resultOp)
            throws BenchmarkResultException
    {
        super(description, resultOp);
        setObjectType(objectType);
        setNumberOfObjects(count);
    }

    /**
     * Constructor
     * 
     * @param doc
     *        (BSON document, mandatory)
     * 
     * @throws BenchmarkResultException
     */
    public ObjectsResultData(Document doc) throws BenchmarkResultException
    {
        super(doc);
        setObjectType(ResultObjectType.valueOf(doc.getString(FIELD_OBJECT_TYPE)));
        setNumberOfObjects(doc.getLong(FIELD_NUMBER_OF_OBJECTS));
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
     * @return total number of objects if descriptive value and object type are
     *         the same, exception else
     * 
     * @throws BenchmarkResultException
     */
    public static ObjectsResultData combine(ObjectsResultData data1,
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
        if (data1.getResultOperation() != data2.getResultOperation())
        {
            throw new BenchmarkResultException(
                    "Data objects must have the same result operation type!");
        }
        long count = data1.getNumberOfObjects() + data2.getNumberOfObjects();
        return new ObjectsResultData(count, data1.getObjectType(),
                data1.getDescription(), data1.getResultOperation());
    }

    @Override
    public Document toDocumentBSON()
    {
        Document doc = getDocumentBSON()
                .append(FIELD_NUMBER_OF_OBJECTS, this.numberOfObjects)
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
